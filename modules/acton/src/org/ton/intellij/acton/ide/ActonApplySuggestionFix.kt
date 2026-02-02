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
            val line = edit.range.start.line
            val startOffset = if (line in 0 until document.lineCount) {
                document.getLineStartOffset(line) + edit.range.start.character
            } else {
                Int.MAX_VALUE
            }
            startOffset
        }

        for (edit in sortedEdits) {
            val startLine = edit.range.start.line
            val endLine = edit.range.end.line
            
            if (startLine !in 0 until document.lineCount || endLine !in 0 until document.lineCount) continue
            
            val lineStartOffset = document.getLineStartOffset(startLine)
            val lineEndOffset = document.getLineStartOffset(endLine)
            
            val startOffset = (lineStartOffset + edit.range.start.character).coerceIn(0, document.textLength)
            val endOffset = (lineEndOffset + edit.range.end.character).coerceIn(0, document.textLength)

            if (startOffset <= endOffset) {
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