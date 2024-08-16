package org.ton.intellij.tolk.editor

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiFile
import org.ton.intellij.tolk.psi.TolkFile

class TolkTypedHandler : TypedHandlerDelegate() {
    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (file !is TolkFile) return Result.CONTINUE
        if (c != '-') return Result.CONTINUE
        val offset = editor.caretModel.offset
        if (StringUtil.endsWith(editor.document.immutableCharSequence, 0, offset + 1, "{-}")) {
            editor.document.insertString(offset, "-")
        }
        return Result.CONTINUE
    }
}
