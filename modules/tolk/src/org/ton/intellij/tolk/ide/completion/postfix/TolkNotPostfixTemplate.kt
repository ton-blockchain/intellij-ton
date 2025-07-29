package org.ton.intellij.tolk.ide.completion.postfix

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateExpressionSelector
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateWithExpressionSelector
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.startOffset
import org.ton.intellij.tolk.psi.TolkExpression

class TolkNotPostfixTemplate : PostfixTemplateWithExpressionSelector(
    "tolk.postfix.not", "not",
    "!expr", getBooleanExpressions(), null
) {
    override fun isApplicable(context: PsiElement, copyDocument: Document, newOffset: Int) =
        TolkPostfixUtil.isExpression(context)

    override fun expandForChooseExpression(expression: PsiElement, editor: Editor) {
        val document = editor.document
        val containOtherExpressionInside = PsiTreeUtil.findChildOfType(expression, TolkExpression::class.java) != null

        if (containOtherExpressionInside) {
            // true && false -> !(true && false)
            document.insertString(expression.startOffset, "!(")
            document.insertString(expression.endOffset + 2, ")")
        } else {
            document.insertString(expression.startOffset, "!")
        }
    }

    companion object {
        private fun getBooleanExpressions(): PostfixTemplateExpressionSelector {
            return findAllExpressions { e -> e is TolkExpression }
        }
    }
}
