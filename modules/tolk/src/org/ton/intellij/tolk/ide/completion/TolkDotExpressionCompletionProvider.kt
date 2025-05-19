package org.ton.intellij.tolk.ide.completion

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
import org.ton.intellij.tolk.psi.TolkDotExpression
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkTypeSymbolElement
import org.ton.intellij.tolk.psi.impl.declaredType
import org.ton.intellij.tolk.psi.impl.hasSelf
import org.ton.intellij.tolk.psi.impl.isDeprecated
import org.ton.intellij.tolk.psi.impl.toLookupElement
import org.ton.intellij.tolk.stub.index.TolkFunctionIndex
import org.ton.intellij.tolk.type.*

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

        if (calledType is TolkStructTy && !isStaticReceiver) {
            val sub = Substitution.instantiate(calledType.psi.declaredType, calledType)
            calledType.psi.structBody?.structFieldList?.forEach { field ->
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

        fun addFunction(function: TolkFunction) {
            val name = function.name ?: return

            if (!prefixMatcher.prefixMatches(name)) return
            if (function.hasSelf != !isStaticReceiver) return
            if (!functions.add(function)) return

            val receiverType =
                function.functionReceiver?.typeExpression?.type?.unwrapTypeAlias()?.actualType() ?: return

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
        }

        TolkFunctionIndex.processAllElements(project, file.resolveScope, ::addFunction)
        TolkFunctionIndex.processAllElements(project, processor = ::addFunction)
    }
}
