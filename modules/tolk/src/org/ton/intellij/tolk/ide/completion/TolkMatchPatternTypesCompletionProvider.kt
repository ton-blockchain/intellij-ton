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
import org.ton.intellij.tolk.psi.TolkMatchPattern
import org.ton.intellij.tolk.psi.TolkReferenceExpression
import org.ton.intellij.tolk.type.*

object TolkMatchPatternTypesCompletionProvider : TolkCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement> =
        psiElement()
            .withParent(TolkReferenceExpression::class.java)
            .withSuperParent(2, TolkMatchPattern::class.java)

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val position = parameters.position
        val matchExpr = position.parentOfType<TolkMatchExpression>() ?: return
        val expr = matchExpr.expression ?: return
        val ctx = TolkCompletionContext(position.parent as? TolkElement)

        val declaredMatchArms = hashSetOf<String>()
        for (arm in matchExpr.matchArmList) {
            declaredMatchArms.add(arm.matchPattern.text)
        }

        val type = expr.type ?: TolkTy.Unknown
        val unwrappedType = type.unwrapTypeAlias()
        if (unwrappedType is TolkTyUnion) {
            for (unionVariant in unwrappedType.variants) {
                val variantText = unionVariant.render()
                if (declaredMatchArms.contains(variantText)) {
                    continue
                }

                val element = when (unionVariant) {
                    is TolkTyStruct    -> unionVariant.psi.toLookupElementBuilder(ctx).forMatchArm()
                    is TolkTyAlias     -> unionVariant.psi.toLookupElementBuilder(ctx).forMatchArm()
                    is TolkPrimitiveTy -> unionVariant.toLookupElement().forMatchArm()
                    else               -> LookupElementBuilder.create(unionVariant.render()).forMatchArm()
                }

                result.addElement(element)
            }
        } else {
            collectLocalVariables(matchExpr) {
                result.addElement(it.toLookupElementBuilder(ctx))
                true
            }
        }

        if (!declaredMatchArms.contains("else")) {
            result.addElement(
                LookupElementBuilder.create("else")
                    .withTailText(" => {}", true)
                    .withInsertHandler(TemplateStringInsertHandler(" => {\n\$END$\n}"))
            )
        }
    }

    fun LookupElementBuilder.forMatchArm(): LookupElementBuilder {
        return this.withTailText(" => {}", true)
            .withInsertHandler(TemplateStringInsertHandler(" => {\n\$END$\n}"))
    }
}
