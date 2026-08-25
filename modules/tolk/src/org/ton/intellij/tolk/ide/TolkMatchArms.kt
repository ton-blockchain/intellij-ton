package org.ton.intellij.tolk.ide

import com.intellij.codeInsight.template.TemplateManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.startOffset
import org.ton.intellij.tolk.psi.TolkMatchArm
import org.ton.intellij.tolk.psi.TolkMatchExpression
import org.ton.intellij.tolk.psi.impl.members
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.tolk.type.TolkTyEnum
import org.ton.intellij.tolk.type.TolkTyUnion
import org.ton.intellij.tolk.type.render

/**
 * Generation of `match` arms shared by the `Fill all cases…` completion
 * ([org.ton.intellij.tolk.ide.completion.TolkAllMatchTypesCompletionProvider]) and the
 * [org.ton.intellij.tolk.ide.intentions.TolkFillMatchArmsIntention] intention.
 */
object TolkMatchArms {
    const val ELSE_PATTERN = "else"

    private const val CARET_MARKER = "\$END$"

    /**
     * Patterns of the arms that [matchExpression] doesn't declare yet, in declaration order,
     * followed by `else` when there is no `else` arm.
     *
     * Empty when the matched expression is neither a union nor an enum, i.e. when there is no
     * fixed set of arms to generate.
     */
    fun missingPatterns(matchExpression: TolkMatchExpression): List<String> {
        val expression = matchExpression.expression ?: return emptyList()
        val declared = matchExpression.matchArmList.mapTo(hashSetOf()) { it.matchPattern.text }

        val missing = when (val type = (expression.type ?: TolkTy.Unknown).unwrapTypeAlias()) {
            is TolkTyUnion -> type.variants.map { it.render() }.filter { it !in declared }

            is TolkTyEnum -> {
                val enumName = type.psi.name ?: return emptyList()
                type.psi.members.mapNotNull { it.name }
                    // an arm may reference a member either as `Red` or as `Color.Red`
                    .filter { it !in declared && "$enumName.$it" !in declared }
                    .map { "$enumName.$it" }
            }

            else -> return emptyList()
        }

        if (ELSE_PATTERN in declared) return missing
        return missing + ELSE_PATTERN
    }

    /**
     * Renders [patterns] as arms with empty block bodies, with [CARET_MARKER] inside the first one.
     */
    fun armsTemplateText(patterns: Collection<String>): String = patterns
        .mapIndexed { index, pattern -> "$pattern => {\n${if (index == 0) CARET_MARKER else ""}\n}" }
        .joinToString("\n")

    /** A single document replacement adding arms to a `match`. */
    data class ArmsEdit(val startOffset: Int, val endOffset: Int, val text: String) {
        /** [text] without the live template markers, for a plain non-interactive insertion. */
        val plainText: String get() = text.replace(CARET_MARKER, "")
    }

    /**
     * Where and what to insert to add arms for [patterns] to [matchExpression].
     *
     * The arms land right where [caretOffset] is whenever the caret stands between the existing
     * arms, and at the end of the `match` otherwise. They never go below an `else` arm, which
     * would make them unreachable.
     */
    fun armsEdit(matchExpression: TolkMatchExpression, patterns: Collection<String>, caretOffset: Int): ArmsEdit? {
        if (patterns.isEmpty()) return null
        val document = matchExpression.containingFile?.viewProvider?.document ?: return null
        val lbrace = matchExpression.lbrace ?: return null
        val rbrace = matchExpression.rbrace ?: return null

        val arms = matchExpression.matchArmList
        val armAtCaret = if (caretOffset in lbrace.endOffset..rbrace.startOffset) {
            arms.firstOrNull { it.startOffset >= caretOffset }
        } else {
            null
        }
        val elseArm = arms.firstOrNull { it.matchPattern.text == ELSE_PATTERN }
        val anchorOffset = listOfNotNull(armAtCaret, elseArm).minOfOrNull { it.startOffset } ?: rbrace.startOffset

        val previousArm = arms.lastOrNull { it.endOffset <= anchorOffset }
        val comma = if (previousArm != null && previousArm.needsTrailingComma()) "," else ""
        val startOffset = previousArm?.endOffset ?: lbrace.endOffset
        val armsText = armsTemplateText(patterns)

        if (!document.charsSequence.subSequence(startOffset, anchorOffset).isBlank()) {
            // a comment sits in between, keep it where it is and squeeze the arms in front of it
            return ArmsEdit(startOffset, startOffset, "$comma\n$armsText")
        }
        return ArmsEdit(startOffset, anchorOffset, "$comma\n$armsText\n" + lineIndent(document, anchorOffset))
    }

    /** Applies [edit] as a live template, so that the caret ends up in the first generated arm. */
    fun applyAsTemplate(project: Project, editor: Editor, edit: ArmsEdit) {
        val document = editor.document
        document.deleteString(edit.startOffset, edit.endOffset)
        PsiDocumentManager.getInstance(project).commitDocument(document)
        editor.caretModel.moveToOffset(edit.startOffset)
        startTemplate(project, editor, edit.text)
    }

    /** Applies [edit] as plain text — used to render the intention preview. */
    fun applyPlain(project: Project, file: PsiFile, edit: ArmsEdit) {
        val document = file.viewProvider.document ?: return
        val text = edit.plainText
        document.replaceString(edit.startOffset, edit.endOffset, text)
        PsiDocumentManager.getInstance(project).commitDocument(document)
        CodeStyleManager.getInstance(project).reformatText(file, edit.startOffset, edit.startOffset + text.length)
    }

    fun startTemplate(project: Project, editor: Editor, templateText: String) {
        val templateManager = TemplateManager.getInstance(project)
        val template = templateManager.createTemplate("match-arms", "tolk", templateText)
        template.isToReformat = true
        templateManager.startTemplate(editor, template)
    }

    /** Indentation of the line [offset] is on, or an empty string when something precedes it there. */
    private fun lineIndent(document: Document, offset: Int): String {
        val lineStart = document.getLineStartOffset(document.getLineNumber(offset))
        val prefix = document.getText(TextRange(lineStart, offset))
        return if (prefix.isBlank()) prefix else ""
    }

    /**
     * A non-block arm (`A => expr`, `A => return x`, `A => throw x`) may only omit the trailing
     * comma when it is the last one, so appending after it requires the comma.
     */
    private fun TolkMatchArm.needsTrailingComma(): Boolean {
        val body = matchBody ?: return false
        return body.blockStatement == null && body.comma == null
    }
}
