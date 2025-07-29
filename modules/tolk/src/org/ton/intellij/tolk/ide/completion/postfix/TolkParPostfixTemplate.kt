package org.ton.intellij.tolk.ide.completion.postfix

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateExpressionSelector
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateWithExpressionSelector
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.startOffset
import org.ton.intellij.tolk.psi.TolkExpression

class TolkParPostfixTemplate : PostfixTemplateWithExpressionSelector(
    "tolk.postfix.par", "par",
    "(expr)", getExpressions(), null
) {
    override fun isApplicable(context: PsiElement, copyDocument: Document, newOffset: Int) =
        TolkPostfixUtil.isExpression(context)

    override fun expandForChooseExpression(expression: PsiElement, editor: Editor) {
        val document = editor.document
        val caret = editor.caretModel.primaryCaret

        document.insertString(expression.endOffset, ")")
        document.insertString(expression.startOffset, "(")
        caret.moveToOffset(expression.endOffset + 2)
    }

    companion object {
        private fun getExpressions(): PostfixTemplateExpressionSelector {
            return findAllExpressions { e -> e is TolkExpression }
        }
    }
}
