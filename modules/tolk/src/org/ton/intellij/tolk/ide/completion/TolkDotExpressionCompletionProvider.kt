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
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.perf
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.declaredType
import org.ton.intellij.tolk.psi.impl.hasDeprecatedAnnotation
import org.ton.intellij.tolk.psi.impl.isStatic
import org.ton.intellij.tolk.psi.impl.receiverTy
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
        result: CompletionResultSet
    ) {
        val file = parameters.originalFile
        val element = parameters.position.parent
        val project = file.project
        val dotExpression = parameters.position.parentOfType<TolkDotExpression>() ?: return
        val left = dotExpression.expression
        val resolvedReceiver = left.reference?.resolve()
        val calledType = left.type?.actualType() ?: return
        val primitiveStaticReceiver = (left as? TolkReferenceExpression)?.let {
            TolkPrimitiveTy.fromReference(it)
        }
        val isStaticReceiver = resolvedReceiver is TolkTypeSymbolElement
        val isBeforeParenthesis = parameters.originalPosition?.let {
            val text = it.containingFile.text
            val offset = it.textOffset + it.textLength
            offset < text.length && text[offset] == '('
        } ?: false

        val completionLimit = REGISTRY_IDE_COMPLETION_VARIANT_LIMIT
        val ctx = TolkCompletionContext(element as? TolkElement)
//        val result = DeferredCompletionResultSet(result)
        val prefixMatcher = result.prefixMatcher
        var addedElements = 0
        fun checkLimit(): Boolean {
            if (addedElements >= completionLimit) {
                result.restartCompletionOnAnyPrefixChange()
                result.addLookupAdvertisement(AnalysisBundle.message("completion.not.all.variants.are.shown"))
                return false
            }
            addedElements++
            return true
        }

        if (!isStaticReceiver && !isBeforeParenthesis) {
            when (calledType) {
                is TolkStructTy -> {
                    val sub = Substitution.instantiate(calledType.psi.declaredType, calledType)
                    for (field in calledType.psi.structBody?.structFieldList.orEmpty()) {
                        if (!checkLimit()) return
                        val lookupElement = field.toLookupElementBuilder(ctx, sub)
                        if (prefixMatcher.prefixMatches(lookupElement)) {
                            result.addElement(lookupElement.toTolkLookupElement(
                                TolkLookupElementData(
                                    elementKind = TolkLookupElementData.ElementKind.FIELD
                                )
                            ))
                        }
                    }
                }

                is TolkTyTypedTuple, is TolkTyTensor -> {
                    val elements = when (calledType) {
                        is TolkTyTypedTuple -> calledType.elements
                        is TolkTyTensor -> calledType.elements
                        else -> emptyList()
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

        val functions = HashSet<TolkFunction>()
        val calledTypeWithoutNull = calledType.actualType().removeNullable()

        fun addFunction(function: TolkFunction): Boolean {
            val name = function.name ?: return true

            if (!prefixMatcher.prefixMatches(name)) return true
            if (!functions.add(function)) return true
            val isStatic = function.isStatic
            if (isStatic != isStaticReceiver && primitiveStaticReceiver == null) return true

            val receiverType =
                function.receiverTy.unwrapTypeAlias().actualType()

            val canBeAssigned = receiverType.canRhsBeAssigned(calledType)

            if (!canBeAssigned && !isStatic && primitiveStaticReceiver != null) return true

            fun canBeAdded(): Boolean {
                if ((canBeAssigned && isStatic == isStaticReceiver) || (receiverType == primitiveStaticReceiver && isStatic)) return true
                if (receiverType is TolkTyParam) return true
                if (receiverType.hasGenerics() &&
                    receiverType is TolkStructTy &&
                    calledType is TolkStructTy &&
                    receiverType.psi.isEquivalentTo(calledType.psi)
                ) return true
                return false
            }

            if (canBeAdded()) {
                if (!checkLimit()) return false
                val lookupElement = function.toLookupElementBuilder(ctx)
                    .withBoldness(receiverType == calledType)
                    .toTolkLookupElement(
                        TolkLookupElementData(
                            isSelfTypeCompatible = function.receiverTy == calledType,
                            isSelfTypeNullableCompatible = function.receiverTy.removeNullable() == calledTypeWithoutNull,
                            isInherentUnionMember = receiverType is TolkTyUnion,
                            isGeneric = receiverType is TolkTyParam,
                            elementKind = when {
                                function.hasDeprecatedAnnotation -> TolkLookupElementData.ElementKind.DEPRECATED
                                isStatic -> TolkLookupElementData.ElementKind.STATIC_FUNCTION
                                else -> TolkLookupElementData.ElementKind.DEFAULT
                            },
                            isDeferredLookup = when (name) {
                                "getDeclaredPackPrefix",
                                "getDeclaredPackPrefixLen",
                                "stackMoveToTop" -> true
                                else -> false
                            }
                        )
                    )

                result.addElement(lookupElement)
            }

            return true
        }

        perf("dot completion global") {
            TolkFunctionIndex.processAllElements(project, processor = ::addFunction)
        }

        if (result is DeferredCompletionResultSet) {
            result.flushDeferredElements()
        }
    }
}
