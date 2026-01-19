package org.ton.intellij.acton.ide

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.util.IntentionName
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.ton.intellij.acton.ActonBundle

class ActonApplySuggestionFix(
    @param:IntentionName private val fixName: String,
    private val edits: List<ActonEdit>,
    private val document: Document,
) : IntentionAction {

    override fun startInWriteAction(): Boolean = true

    override fun getText(): String = ActonBundle.message("intention.name.apply.fix", fixName)

    override fun getFamilyName(): String = ActonBundle.message("intention.family.name.apply.acton.fix")

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean = true

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val sortedEdits = edits.sortedByDescending { edit ->
            val startOffset = document.getLineStartOffset(edit.range.start.line) + edit.range.start.character
            startOffset
        }

        for (edit in sortedEdits) {
            val startOffset = document.getLineStartOffset(edit.range.start.line) + edit.range.start.character
            val endOffset = document.getLineStartOffset(edit.range.end.line) + edit.range.end.character

            if (startOffset >= 0 && endOffset >= 0 &&
                startOffset <= document.textLength && endOffset <= document.textLength &&
                startOffset <= endOffset
            ) {
                document.replaceString(startOffset, endOffset, edit.newText)
            }
        }
    }

    companion object {
        fun fromFix(fix: ActonFix, document: Document): ActonApplySuggestionFix? {
            if (fix.edits.isEmpty()) return null

            return ActonApplySuggestionFix(
                fixName = fix.message,
                edits = fix.edits,
                document = document
            )
        }
    }
}