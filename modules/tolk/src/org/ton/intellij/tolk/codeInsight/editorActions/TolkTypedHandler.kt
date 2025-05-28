package org.ton.intellij.tolk.codeInsight.editorActions

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.DocumentUtil
import org.ton.intellij.tolk.psi.TolkDotExpression
import org.ton.intellij.tolk.psi.TolkFile

class TolkTypedHandler : TypedHandlerDelegate() {
    override fun checkAutoPopup(charTyped: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (file !is TolkFile) return Result.CONTINUE
        if (charTyped == '@') {
            // Trigger auto-popup for annotations
            AutoPopupController.getInstance(project).autoPopupMemberLookup(editor, null);
            return Result.STOP
        }
        return Result.CONTINUE
    }

    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (file !is TolkFile) return Result.CONTINUE

        when(c) {
            '.' -> {
                if (autoIndentCase(editor, project, file, TolkDotExpression::class.java)) return Result.STOP
            }
        }
        return Result.CONTINUE
    }

    private fun autoIndentCase(
        editor: Editor,
        project: Project,
        file: PsiFile,
        klass: Class<*>,
        forFirstElement: Boolean = true,
    ): Boolean {
        val offset = editor.caretModel.offset
        PsiDocumentManager.getInstance(project).commitDocument(editor.document)
        val currElement = file.findElementAt(offset - 1)
        if (currElement != null) {
            // Should be applied only if there's nothing but the whitespace in line before the element
            val prevLeaf = PsiTreeUtil.prevLeaf(currElement)
            if (forFirstElement && !(prevLeaf is PsiWhiteSpace && prevLeaf.textContains('\n'))) {
                return false
            }

            val parent = currElement.parent
            if (klass.isInstance(parent)) {
                val curElementLength = currElement.text.length
                if (offset < curElementLength) return false
                if (forFirstElement) {
                    CodeStyleManager.getInstance(project).adjustLineIndent(file, offset - curElementLength)
                } else {
                    val document = PsiDocumentManager.getInstance(project).getDocument(file)
                    if (document != null) {
                        CodeStyleManager.getInstance(project).adjustLineIndent(document, DocumentUtil.getLineStartOffset(offset, document))
                    }
                }

                return true
            }
        }

        return false
    }
}
