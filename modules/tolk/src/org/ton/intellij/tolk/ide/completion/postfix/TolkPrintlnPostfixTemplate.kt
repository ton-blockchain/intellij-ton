package org.ton.intellij.tolk.ide.completion.postfix

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplate
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.startOffset
import org.ton.intellij.tolk.psi.TolkExpressionStatement
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.stub.index.TolkFunctionIndex

class TolkPrintlnPostfixTemplate : PostfixTemplate(
    "tolk.postfix.println", "println",
    "println(expr);", null
) {
    override fun isApplicable(context: PsiElement, copyDocument: Document, newOffset: Int): Boolean {
        val file = context.containingFile.originalFile as? TolkFile ?: return false
        if (!file.isTestFile() && !file.isInScriptsFolder()) return false
        return TolkPostfixUtil.isExpression(context)
    }

    override fun expand(context: PsiElement, editor: Editor) {
        val project = context.project
        val element = context.parentOfType<TolkExpressionStatement>() ?: return
        val file = element.containingFile as? TolkFile ?: return
        val document = editor.document

        document.insertString(element.startOffset, "println(")
        TolkPostfixUtil.startTemplate(");\$END$", project, editor)

        WriteCommandAction.runWriteCommandAction(project, "Add Import For Function", null, {
            TolkFunctionIndex.processElements(project, "println", GlobalSearchScope.allScope(project)) { func ->
                val printlnFile = func.containingFile as? TolkFile ?: return@processElements false
                file.import(printlnFile)
                false
            }
        })
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)
    }
}
