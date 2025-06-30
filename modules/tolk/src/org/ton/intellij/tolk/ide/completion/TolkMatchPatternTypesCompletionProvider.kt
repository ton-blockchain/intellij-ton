package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.psi.TolkElement
import org.ton.intellij.tolk.psi.TolkMatchExpression
import org.ton.intellij.tolk.psi.TolkMatchPatternReference
import org.ton.intellij.tolk.type.*

object TolkMatchPatternTypesCompletionProvider : TolkCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement> =
        psiElement()
            .withParent(TolkMatchPatternReference::class.java)

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val position = parameters.position
        val matchExpr = position.parentOfType<TolkMatchExpression>() ?: return
        val expr = matchExpr.expression
        val ctx = TolkCompletionContext(position.parent as? TolkElement)
        if (expr != null) {
            val type = expr.type ?: TolkTy.Unknown
            val unwrappedType = type.unwrapTypeAlias()
            if (unwrappedType is TolkTyUnion) {
                unwrappedType.variants.forEach { unionVariant ->
                    when(unionVariant) {
                        is TolkTyStruct -> result.addElement(unionVariant.psi.toLookupElementBuilder(ctx))
                        is TolkTyAlias -> result.addElement(unionVariant.psi.toLookupElementBuilder(ctx))
                        is TolkPrimitiveTy -> result.addElement(unionVariant.toLookupElement())
                        else -> result.addElement(
                            LookupElementBuilder.create(unionVariant.render())
                        )
                    }
                }
            } else {
                collectLocalVariables(matchExpr) {
                    result.addElement(it.toLookupElementBuilder(ctx))
                    true
                }
            }
        }
    }
}
