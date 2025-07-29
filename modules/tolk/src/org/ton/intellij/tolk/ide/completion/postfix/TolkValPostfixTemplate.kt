package org.ton.intellij.tolk.ide.completion.postfix

import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplate
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.startOffset
import org.ton.intellij.tolk.psi.TolkExpressionStatement

class TolkValPostfixTemplate : PostfixTemplate(
    "tolk.postfix.val", "val",
    "val name = value", null
) {
    override fun isApplicable(context: PsiElement, copyDocument: Document, newOffset: Int) =
        TolkPostfixUtil.isExpression(context) &&
                TolkPostfixUtil.notInsideVarDeclaration(context)

    override fun expand(context: PsiElement, editor: Editor) {
        val element = context.parentOfType<TolkExpressionStatement>() ?: return
        val caret = editor.caretModel.primaryCaret
        val offset = element.startOffset

        editor.document.insertString(element.endOffset, ";")
        editor.document.insertString(offset, " = ")
        caret.moveToOffset(offset)

        TolkPostfixUtil.startTemplate("val \$name$", context.project, editor, "name" to ConstantNode("name"))
    }
}
