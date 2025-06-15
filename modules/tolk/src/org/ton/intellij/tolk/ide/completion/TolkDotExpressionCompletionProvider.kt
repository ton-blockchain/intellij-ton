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
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.perf
import org.ton.intellij.tolk.psi.TolkDotExpression
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkReferenceExpression
import org.ton.intellij.tolk.psi.TolkTypeSymbolElement
import org.ton.intellij.tolk.psi.impl.declaredType
import org.ton.intellij.tolk.psi.impl.isStatic
import org.ton.intellij.tolk.psi.impl.receiverTy
import org.ton.intellij.tolk.psi.impl.toLookupElement
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
        val project = file.project
        val dotExpression = parameters.position.parentOfType<TolkDotExpression>() ?: return
        val left = dotExpression.expression
        val resolvedReceiver = left.reference?.resolve()
        val calledType = left.type?.actualType() ?: return
        val possiblePrimitiveCalledType = (left as? TolkReferenceExpression)?.let {
            TolkPrimitiveTy.fromReference(it)
        }
        val isStaticReceiver = resolvedReceiver is TolkTypeSymbolElement
        val isBeforeParenthesis = parameters.originalPosition?.let {
            val text = it.containingFile.text
            val offset = it.textOffset + it.textLength
            offset < text.length && text[offset] == '('
        } ?: false

        val completionLimit = REGISTRY_IDE_COMPLETION_VARIANT_LIMIT
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
            when(calledType) {
                is TolkStructTy -> {
                    val sub = Substitution.instantiate(calledType.psi.declaredType, calledType)
                    for (field in calledType.psi.structBody?.structFieldList.orEmpty()) {
                        if (!checkLimit()) return
                        result.addElement(
                            PrioritizedLookupElement.withPriority(
                                LookupElementBuilder
                                    .createWithIcon(field)
                                    .withTypeText(field.type?.substitute(sub)?.render()),
                                TolkCompletionPriorities.INSTANCE_FIELD
                            )
                        )
                    }
                }
                is TolkTypedTupleTy, is TolkTensorTy -> {
                    val elements = when (calledType) {
                        is TolkTypedTupleTy -> calledType.elements
                        is TolkTensorTy -> calledType.elements
                        else -> emptyList()
                    }
                    for ((index, element) in elements.withIndex()) {
                        if (!checkLimit()) return
                        result.addElement(
                            PrioritizedLookupElement.withPriority(
                                LookupElementBuilder
                                    .create("$index")
                                    .withTypeText(element.render()),
                                TolkCompletionPriorities.INSTANCE_FIELD
                            )
                        )
                    }
                }
            }
        }

        val prefixMatcher = result.prefixMatcher
        val functions = HashSet<TolkFunction>()

        fun addFunction(function: TolkFunction): Boolean {
            val name = function.name ?: return true

            if (!prefixMatcher.prefixMatches(name)) return true
            if (!functions.add(function)) return true
            val isStatic = function.isStatic
            if (isStatic != isStaticReceiver && possiblePrimitiveCalledType == null) return true

            val receiverType =
                function.receiverTy.unwrapTypeAlias().actualType()

            val canBeAssigned = receiverType.canRhsBeAssigned(calledType)

            if (!canBeAssigned && !isStatic && possiblePrimitiveCalledType != null) return true

            fun canBeAdded(): Boolean {
                if ((canBeAssigned && isStatic == isStaticReceiver) || (receiverType == possiblePrimitiveCalledType && isStatic)) return true
                if (receiverType is TolkTypeParameterTy) return true
                if (receiverType.hasGenerics() &&
                    receiverType is TolkStructTy &&
                    calledType is TolkStructTy &&
                    receiverType.psi.isEquivalentTo(calledType.psi)
                ) return true
                return false
            }

            if (canBeAdded()) {
                if (!checkLimit()) return false
                val deprecatedPriority = if (function.isDeprecated) 0.0 else -TolkCompletionPriorities.DEPRECATED
                val memberPriority = if (isStaticReceiver) TolkCompletionPriorities.STATIC_FUNCTION
                else TolkCompletionPriorities.INSTANCE_METHOD
                var priority = memberPriority + deprecatedPriority
                if (canBeAssigned) priority += 1
                result.addElement(
                    PrioritizedLookupElement.withPriority(
                        function.toLookupElement(),
                        priority
                    )
                )
            }

            return true
        }

        perf("dot completion global") {
            TolkFunctionIndex.processAllElements(project, processor = ::addFunction)
        }
    }
}
