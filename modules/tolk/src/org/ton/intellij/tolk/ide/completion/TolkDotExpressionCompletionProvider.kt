package org.ton.intellij.tolk.ide.completion

import com.intellij.analysis.AnalysisBundle
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import com.intellij.ui.JBColor
import com.intellij.util.ProcessingContext
import com.intellij.util.applyIf
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.*
import org.ton.intellij.tolk.psi.reference.collectMethodCandidates
import org.ton.intellij.tolk.stub.index.TolkFunctionIndex
import org.ton.intellij.tolk.type.*
import org.ton.intellij.util.REGISTRY_IDE_COMPLETION_VARIANT_LIMIT

object TolkDotExpressionCompletionProvider : TolkCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement>
        get() = PlatformPatterns.psiElement()
            .afterLeaf(".")
            .withSuperParent(2, TolkDotExpression::class.java)
            .andNot(
                PlatformPatterns.psiElement().afterLeaf(
                    PlatformPatterns.psiElement().withText(StandardPatterns.string().matches("\\d+"))
                )
            )

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val file = parameters.originalFile
        val project = file.project

        val dotExpression = parameters.position.parentOfType<TolkDotExpression>() ?: return
        val qualifier = dotExpression.expression
        val resolvedReceiver = qualifier.reference?.resolve()
        val calledType = qualifier.type?.actualType() ?: return

        val isStaticReceiver = qualifier !is TolkDotExpression && resolvedReceiver == null || resolvedReceiver is TolkTypeSymbolElement
        val isBeforeParenthesis = parameters.originalPosition?.let {
            val text = it.containingFile.text
            val offset = it.textOffset + it.textLength
            offset < text.length && text[offset] == '('
        } ?: false

        val completionLimit = REGISTRY_IDE_COMPLETION_VARIANT_LIMIT
        val element = parameters.position.parent
        val ctx = TolkCompletionContext(element as? TolkElement)

        val prefixMatcher = result.prefixMatcher

        var addedElements = 0
        fun checkLimit(): Boolean {
            if (addedElements >= completionLimit) {
                result.restartCompletionOnAnyPrefixChange()
                result.addLookupAdvertisement(AnalysisBundle.message("completion.not.all.variants.are.shown"))
                return false
            }
            @Suppress("AssignedValueIsNeverRead")
            addedElements++
            return true
        }

        if (!isStaticReceiver && !isBeforeParenthesis) {
            when (calledType) {
                is TolkTyStruct                      -> {
                    val sub = Substitution.instantiate(calledType.psi.declaredType, calledType)
                    for (field in calledType.psi.structBody?.structFieldList.orEmpty()) {
                        if (!checkLimit()) return
                        val lookupElement = field.toLookupElementBuilder(ctx, sub)
                        if (prefixMatcher.prefixMatches(lookupElement)) {
                            result.addElement(
                                lookupElement.toTolkLookupElement(
                                    TolkLookupElementData(
                                        elementKind = TolkLookupElementData.ElementKind.FIELD
                                    )
                                )
                            )
                        }
                    }
                }

                is TolkTyTypedTuple, is TolkTyTensor -> {
                    val elements = when (calledType) {
                        is TolkTyTypedTuple -> calledType.elements
                        is TolkTyTensor     -> calledType.elements
                        else                -> emptyList()
                    }
                    for ((index, element) in elements.withIndex()) {
                        if (!checkLimit()) return
                        val lookupElement = LookupElementBuilder
                            .create(index)
                            .bold()
                            .withTypeText(element.render())
                        if (prefixMatcher.prefixMatches(lookupElement)) {
                            result.addElement(lookupElement)
                        }
                    }
                }
            }
        }

        val unwrappedCalledType = calledType.unwrapTypeAlias()
        if (isStaticReceiver && unwrappedCalledType is TolkTyEnum) {
            for (field in unwrappedCalledType.psi.members) {
                if (!checkLimit()) return
                val lookupElement = field.toLookupElementBuilder(ctx)
                if (prefixMatcher.prefixMatches(lookupElement)) {
                    result.addElement(
                        lookupElement.toTolkLookupElement(
                            TolkLookupElementData(
                                elementKind = TolkLookupElementData.ElementKind.FIELD
                            )
                        )
                    )
                }
            }
        }

        val calledTypeWithoutNull = calledType.actualType().removeNullable()
        val currentFile = file as? TolkFile ?: return

        val methodsForCompletion = mutableListOf<TolkFunction>()
        TolkFunctionIndex.processAllElements(project, processor = { function ->
            val name = function.name ?: return@processAllElements true
            if (!prefixMatcher.prefixMatches(name)) return@processAllElements true
            methodsForCompletion.add(function)
            true
        })

        val elements = collectMethodCandidates(calledType, methodsForCompletion, forCompletion = true)

        for ((function) in elements) {
            if (!checkLimit()) break

            val name = function.name ?: continue
            val isStatic = function.isStatic
            val receiverType = function.receiverTy.unwrapTypeAlias().actualType()
            val isResolved = currentFile.resolveSymbols(name).contains(function)

            // don't complete static methods for instance expression
            if (!isStaticReceiver && isStatic) continue

            val lookupElement = function.toLookupElementBuilder(ctx)
                .withBoldness(receiverType == calledType)
                .applyIf(isStaticReceiver && !isStatic) {
                    withItemTextForeground(JBColor.GRAY)
                }
                .toTolkLookupElement(
                    TolkLookupElementData(
                        isSelfTypeCompatible = function.receiverTy == calledType,
                        isSelfTypeNullableCompatible = function.receiverTy.removeNullable() == calledTypeWithoutNull,
                        isInherentUnionMember = receiverType is TolkTyUnion,
                        isGeneric = receiverType is TolkTyParam,
                        elementKind = when {
                            !isResolved                      -> TolkLookupElementData.ElementKind.FROM_UNRESOLVED_IMPORT
                            function.isEntryPoint            -> TolkLookupElementData.ElementKind.ENTRY_POINT_FUNCTION
                            function.hasDeprecatedAnnotation -> TolkLookupElementData.ElementKind.DEPRECATED
                            isStatic                         -> TolkLookupElementData.ElementKind.STATIC_FUNCTION
                            else                             -> TolkLookupElementData.ElementKind.DEFAULT
                        },
                        isDeferredLookup = when (name) {
                            "getDeclaredPackPrefix",
                            "getDeclaredPackPrefixLen",
                            "stackMoveToTop",
                                 -> true

                            else -> false
                        }
                    )
                )

            result.addElement(lookupElement)
        }

        if (result is DeferredCompletionResultSet) {
            result.flushDeferredElements()
        }
    }
}
