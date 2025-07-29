package org.ton.intellij.tolk.ide.completion.postfix

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplate
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.startOffset
import org.ton.intellij.tolk.psi.TolkExpressionStatement

class TolkIfPostfixTemplate : PostfixTemplate(
    "tolk.postfix.if", "if",
    "if (cond) {}", null
) {
    override fun isApplicable(context: PsiElement, copyDocument: Document, newOffset: Int) =
        TolkPostfixUtil.isExpression(context)

    override fun expand(context: PsiElement, editor: Editor) {
        val element = context.parentOfType<TolkExpressionStatement>() ?: return
        val document = editor.document

        document.insertString(element.startOffset, "if (")
        TolkPostfixUtil.startTemplate(") {\n\$END$\n}", context.project, editor)
    }
}
