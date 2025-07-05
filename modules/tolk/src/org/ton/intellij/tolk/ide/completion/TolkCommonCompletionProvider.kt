package org.ton.intellij.tolk.ide.completion

import com.intellij.analysis.AnalysisBundle
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.*
import org.ton.intellij.tolk.stub.index.TolkNamedElementIndex
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.util.REGISTRY_IDE_COMPLETION_VARIANT_LIMIT
import org.ton.intellij.util.parentOfType
import org.ton.intellij.util.psiElement

object TolkCommonCompletionProvider : TolkCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement> =
        psiElement<PsiElement>()
            .withParent(
                psiElement<TolkReferenceExpression>()
//                    .andNot(
//                        psiElement<TolkReferenceExpression>()
//                            .withParent(TolkDotExpression::class.java)
//                    )
            )
            .andNot(
                PlatformPatterns.psiElement().afterLeaf(
                    PlatformPatterns.psiElement().withText(StandardPatterns.string().matches("\\d+"))
                )
            )


    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val position = parameters.position
        val element = position.parent as TolkReferenceExpression
        val elementParent = element.parent
        if (position != element.identifier) return

        val project = parameters.position.project
        val currentFile = element.containingFile as? TolkFile ?: return

        var expectType: TolkTy? = null
        if (elementParent is TolkMatchPattern) {
            val matchExpr = elementParent.parentOfType<TolkMatchExpression>()
            if (matchExpr != null) {
                expectType = matchExpr.expression?.type
            }
        }

        val ctx = TolkCompletionContext(element)
        val completionLimit = REGISTRY_IDE_COMPLETION_VARIANT_LIMIT
        var addedElements = 0

        //        val result = DeferredCompletionResultSet(result)
        fun checkLimit(): Boolean {
            if (addedElements >= completionLimit) {
                result.restartCompletionOnAnyPrefixChange()
                result.addLookupAdvertisement(AnalysisBundle.message("completion.not.all.variants.are.shown"))
                return false
            }
            addedElements++
            return true
        }

        if (!collectLocalVariables(element) { localSymbol ->
                if (!checkLimit()) return@collectLocalVariables false
                result.addElement(
                    localSymbol.toLookupElementBuilder(ctx)
                        .toTolkLookupElement(
                            TolkLookupElementData(
                                isLocal = true,
                                elementKind = TolkLookupElementData.ElementKind.VARIABLE
                            )
                        )
                )
                true
            }
        ) {
            return
        }

        fun TolkTy?.canAddElement(elementType: TolkTy?): Boolean {
            if (this == null) return true
            if (elementType == null) return true
            return canRhsBeAssigned(elementType)
        }

        if (expectType.canAddElement(TolkTy.Null)) {
            result.addElement(
                PrioritizedLookupElement.withPriority(
                    LookupElementBuilder.create("null").bold(),
                    TolkCompletionPriorities.KEYWORD
                )
            )
        }
        if (expectType.canAddElement(TolkTy.Bool)) {
            result.addElement(
                PrioritizedLookupElement.withPriority(
                    LookupElementBuilder.create("true").bold(),
                    TolkCompletionPriorities.KEYWORD
                )
            )
            result.addElement(
                PrioritizedLookupElement.withPriority(
                    LookupElementBuilder.create("false").bold(),
                    TolkCompletionPriorities.KEYWORD
                )
            )
        }

        val prefixMatcher = result.prefixMatcher

        fun processNamedElement(element: TolkSymbolElement): Boolean {
            val name = element.name ?: return true
            if (!prefixMatcher.prefixMatches(name)) return true

            if (element is TolkFunction && element.hasReceiver) return true
            if (!checkLimit()) return false
            val isResolved = currentFile.resolveSymbols(name).contains(element)
            when (element) {
                is TolkFunction -> {
                    if (!checkLimit()) return false
                    if (element.isEntryPoint) return true
                    if (!expectType.canAddElement(element.declaredType.returnType)) return true
                    val lookupElement = element.toLookupElementBuilder(ctx)
                        .toTolkLookupElement(
                            TolkLookupElementData(
                                isDeferredLookup = when (name) {
                                    "getDeclaredPackPrefix",
                                    "getDeclaredPackPrefixLen",
                                    "stackMoveToTop" -> true
                                    else -> false
                                },
                                elementKind = when {
                                    !isResolved -> TolkLookupElementData.ElementKind.FROM_UNRESOLVED_IMPORT
                                    element.isEntryPoint -> TolkLookupElementData.ElementKind.ENTRY_POINT_FUNCTION
                                    element.hasDeprecatedAnnotation -> TolkLookupElementData.ElementKind.DEPRECATED
                                    element.isStatic -> TolkLookupElementData.ElementKind.STATIC_FUNCTION
                                    else -> TolkLookupElementData.ElementKind.DEFAULT
                                },
                            )
                        )
                    result.addElement(lookupElement)
                }

                is TolkConstVar,
                is TolkGlobalVar,
                is TolkTypeDef,
                is TolkStruct -> {
                    if (!expectType.canAddElement(element.type)) return true
                    val lookupElement = element
                        .toLookupElementBuilder(ctx)
                        .toTolkLookupElement(
                            TolkLookupElementData(
                                elementKind = when {
                                    !isResolved -> TolkLookupElementData.ElementKind.FROM_UNRESOLVED_IMPORT
                                    element.annotations.hasDeprecatedAnnotation() -> TolkLookupElementData.ElementKind.DEPRECATED
                                    else -> TolkLookupElementData.ElementKind.DEFAULT
                                }
                            )
                        )
                    result.addElement(lookupElement)
                }
            }
            return true
        }

        if (!TolkNamedElementIndex.processAllElements(project) {
                if (it is TolkSymbolElement) {
                    processNamedElement(it)
                } else {
                    // Skip non-symbol elements
                    true
                }
            }) return

        if (result is DeferredCompletionResultSet) {
            result.flushDeferredElements()
        }
    }
}

fun collectLocalVariables(
    startFrom: PsiElement,
    processor: (TolkLocalSymbolElement) -> Boolean
): Boolean {
    var exitFromFunction = false
    val result = PsiTreeUtil.treeWalkUp(startFrom, null) { scope, lastParent ->
        if (scope is TolkFunction) {
            val parameterList = scope.parameterList
            if (parameterList != null) {
                parameterList.parameterList.forEach {
                    if (!processor(it)) return@treeWalkUp false
                }
                parameterList.selfParameter?.let {
                    if (!processor(it)) return@treeWalkUp false
                }
            }
            exitFromFunction = true
            return@treeWalkUp false
        }
        if (scope is TolkCatch && lastParent is TolkBlockStatement) {
            scope.catchParameterList.forEach { catchParameter ->
                if (!processor(catchParameter)) return@treeWalkUp false
            }
        }
        if (scope is TolkInferenceContextOwner) {
            return@treeWalkUp false
        }
        if (scope is TolkBlockStatement) {
            scope.statementList.forEach { statement ->
                if (statement == lastParent) return@treeWalkUp true
                if (statement !is TolkExpressionStatement) return@forEach
                val expression = statement.expression
                if (expression is TolkVarExpression) {
                    fun processVarDefinition(varDefinition: TolkVarDefinition?): Boolean {
                        return when (varDefinition) {
                            is TolkVar ->
                                processor(varDefinition)

                            is TolkVarTuple -> {
                                varDefinition.varDefinitionList.forEach {
                                    if (!processVarDefinition(it)) return false
                                }
                                true
                            }

                            is TolkVarTensor -> {
                                varDefinition.varDefinitionList.forEach {
                                    if (!processVarDefinition(it)) return false
                                }
                                true
                            }

                            is TolkVarParen -> {
                                varDefinition.varDefinition?.let {
                                    if (!processVarDefinition(it)) return false
                                }
                                true
                            }

                            else -> true
                        }
                    }
                    if (!processVarDefinition(expression.varDefinition)) return@treeWalkUp false
                }
            }
        }

        true
    }
    return exitFromFunction || result
}
