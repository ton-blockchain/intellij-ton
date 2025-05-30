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
import org.ton.intellij.tolk.psi.TolkTypeSymbolElement
import org.ton.intellij.tolk.psi.impl.declaredType
import org.ton.intellij.tolk.psi.impl.hasSelf
import org.ton.intellij.tolk.psi.impl.isDeprecated
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
        val isStaticReceiver = resolvedReceiver is TolkTypeSymbolElement

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

        if (calledType is TolkStructTy && !isStaticReceiver) {
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

        val prefixMatcher = result.prefixMatcher
        val functions = HashSet<TolkFunction>()

        fun addFunction(function: TolkFunction): Boolean {
            val name = function.name ?: return true
            if (!prefixMatcher.prefixMatches(name)) return true
            if (!functions.add(function)) return true
            if (function.hasSelf != !isStaticReceiver) return true

            val receiverType =
                function.receiverTy.unwrapTypeAlias().actualType()

            fun canBeAdded(): Boolean {
                if (receiverType.canRhsBeAssigned(calledType)) return true
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
                val priority = memberPriority + deprecatedPriority
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
