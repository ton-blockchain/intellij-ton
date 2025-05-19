package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.ide.completion.TolkCommonCompletionProvider.TolkCompletionContext
import org.ton.intellij.tolk.psi.TolkStructExpression
import org.ton.intellij.tolk.psi.TolkStructExpressionField
import org.ton.intellij.tolk.psi.impl.structFields
import org.ton.intellij.tolk.type.TolkStructTy

object TolkExpressionFieldProvider : TolkCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement> = psiElement()
        .withParent(TolkStructExpressionField::class.java)

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val element = parameters.position
        val parent = element.parent as? TolkStructExpressionField ?: return
        if (parent.identifier != element) return

        val structExpr = parent.parentOfType<TolkStructExpression>() ?: return
        val structTy = structExpr.type?.unwrapTypeAlias() as? TolkStructTy ?: return
        val ctx = TolkCompletionContext(parent)

        structTy.psi.structFields.forEach { field ->
            result.addElement(
                field.toLookupElementBuilder(ctx, true)
            )
        }
    }
}
