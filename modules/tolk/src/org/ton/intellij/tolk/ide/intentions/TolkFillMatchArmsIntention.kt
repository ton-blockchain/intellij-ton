package org.ton.intellij.tolk.ide.intentions

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.ton.intellij.tolk.TolkBundle
import org.ton.intellij.tolk.ide.TolkMatchArms
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.TolkMatchBody
import org.ton.intellij.tolk.psi.TolkMatchExpression

class TolkFillMatchArmsIntention : PsiElementBaseIntentionAction() {
    override fun getFamilyName(): String = TolkBundle.message("intention.fill.match.arms.family.name")
    override fun getText(): String = TolkBundle.message("intention.fill.match.arms.text")

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        if (editor == null) return false
        val matchExpression = findMatchExpression(element) ?: return false
        return TolkMatchArms.missingPatterns(matchExpression).isNotEmpty()
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        if (editor == null) return
        TolkMatchArms.applyAsTemplate(project, editor, armsEdits(editor, element))
    }

    override fun generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo {
        val element = file.findElementAt(editor.caretModel.offset) ?: return IntentionPreviewInfo.EMPTY
        val edits = armsEdits(editor, element)
        if (edits.isEmpty()) return IntentionPreviewInfo.EMPTY
        TolkMatchArms.applyPlain(project, file, edits)
        return IntentionPreviewInfo.DIFF
    }

    private fun armsEdits(editor: Editor, element: PsiElement): List<TolkMatchArms.ArmsEdit> {
        val matchExpression = findMatchExpression(element) ?: return emptyList()
        val patterns = TolkMatchArms.missingPatterns(matchExpression)
        return TolkMatchArms.armsEdits(matchExpression, patterns, editor.caretModel.offset)
    }

    /**
     * The innermost `match` the caret belongs to, unless the caret already sits inside one of its
     * arm bodies — there the user is editing that arm, not the `match` itself.
     */
    private fun findMatchExpression(element: PsiElement): TolkMatchExpression? {
        var current: PsiElement? = element
        while (current != null && current !is TolkFile) {
            if (current is TolkMatchBody) return null
            if (current is TolkMatchExpression) return current
            current = current.parent
        }
        return null
    }
}
