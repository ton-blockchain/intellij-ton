package org.ton.intellij.tolk.ide

import com.intellij.codeInsight.template.TemplateManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.startOffset
import org.ton.intellij.tolk.psi.TolkMatchArm
import org.ton.intellij.tolk.psi.TolkMatchExpression
import org.ton.intellij.tolk.psi.TolkMatchPattern
import org.ton.intellij.tolk.psi.TolkStruct
import org.ton.intellij.tolk.psi.TolkTypeDef
import org.ton.intellij.tolk.psi.TolkTypeSymbolElement
import org.ton.intellij.tolk.psi.impl.members
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.tolk.type.TolkTyAlias
import org.ton.intellij.tolk.type.TolkTyEnum
import org.ton.intellij.tolk.type.TolkTyPsiHolder
import org.ton.intellij.tolk.type.TolkTyStruct
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
        val arms = matchExpression.matchArmList
        val declaredTexts = arms.mapTo(hashSetOf()) { it.matchPattern.text }

        val missing = when (val type = (expression.type ?: TolkTy.Unknown).unwrapTypeAlias()) {
            is TolkTyUnion -> {
                val declared = DeclaredVariants(arms, declaredTexts)
                type.variants.filterNot { declared.covers(it) }.map { it.render() }
            }

            is TolkTyEnum -> {
                val enumName = type.psi.name ?: return emptyList()
                type.psi.members.mapNotNull { it.name }
                    // an arm may reference a member either as `Red` or as `Color.Red`
                    .filter { it !in declaredTexts && "$enumName.$it" !in declaredTexts }
                    .map { "$enumName.$it" }
            }

            else -> return emptyList()
        }

        if (ELSE_PATTERN in declaredTexts) return missing
        return missing + ELSE_PATTERN
    }

    /**
     * Renders [patterns] as arms with empty block bodies, with [CARET_MARKER] inside the first one
     * when [withCaretMarker] is set.
     */
    fun armsTemplateText(patterns: Collection<String>, withCaretMarker: Boolean = true): String = patterns
        .mapIndexed { index, pattern ->
            "$pattern => {\n${if (index == 0 && withCaretMarker) CARET_MARKER else ""}\n}"
        }
        .joinToString("\n")

    /** A single document replacement adding arms to a `match`. */
    data class ArmsEdit(val startOffset: Int, val endOffset: Int, val text: String) {
        /** [text] without the live template markers, for a plain non-interactive insertion. */
        val plainText: String get() = text.replace(CARET_MARKER, "")
    }

    /**
     * Where and what to insert to add arms for [patterns] to [matchExpression], in document order.
     *
     * The arms for the missing variants land right where [caretOffset] is whenever the caret stands
     * between the existing arms, and at the end of the `match` otherwise. A generated `else` always
     * goes after every other arm — anywhere above it, it would make the arms below unreachable.
     */
    fun armsEdits(
        matchExpression: TolkMatchExpression,
        patterns: Collection<String>,
        caretOffset: Int,
    ): List<ArmsEdit> {
        if (patterns.isEmpty()) return emptyList()
        val document = matchExpression.containingFile?.viewProvider?.document ?: return emptyList()
        val lbrace = matchExpression.lbrace ?: return emptyList()
        val rbrace = matchExpression.rbrace ?: return emptyList()

        val arms = matchExpression.matchArmList
        val variants = patterns.filter { it != ELSE_PATTERN }
        // an `else` is missing exactly when the `match` has no `else` arm, so `tailOffset` is the
        // closing brace whenever an `else` has to be generated
        val tailOffset = arms.firstOrNull { it.matchPattern.text == ELSE_PATTERN }?.startOffset ?: rbrace.startOffset
        val caretAnchor = if (caretOffset in lbrace.endOffset..rbrace.startOffset) {
            arms.firstOrNull { it.startOffset >= caretOffset }?.startOffset
        } else {
            null
        }
        val variantsOffset = minOf(caretAnchor ?: tailOffset, tailOffset)

        val context = EditContext(document, arms, lbrace.endOffset)
        return when {
            variants.isEmpty() -> listOf(context.edit(listOf(ELSE_PATTERN), tailOffset))
            ELSE_PATTERN !in patterns -> listOf(context.edit(variants, variantsOffset))
            variantsOffset == tailOffset -> listOf(context.edit(variants + ELSE_PATTERN, variantsOffset))
            else -> listOf(
                context.edit(variants, variantsOffset),
                context.edit(listOf(ELSE_PATTERN), tailOffset, withCaretMarker = false),
            )
        }
    }

    /**
     * Applies [edits], turning the one holding the caret marker into a live template so that the
     * caret ends up in the first generated arm.
     */
    fun applyAsTemplate(project: Project, editor: Editor, edits: List<ArmsEdit>) {
        val templateEdit = edits.firstOrNull() ?: return
        val document = editor.document

        // the trailing edits go first, so that the offsets of the template one stay valid
        edits.drop(1).sortedByDescending { it.startOffset }.forEach {
            document.replaceString(it.startOffset, it.endOffset, it.plainText)
        }
        document.deleteString(templateEdit.startOffset, templateEdit.endOffset)
        PsiDocumentManager.getInstance(project).commitDocument(document)

        editor.caretModel.moveToOffset(templateEdit.startOffset)
        startTemplate(project, editor, templateEdit.text)
    }

    /** Applies [edits] as plain text — used to render the intention preview. */
    fun applyPlain(project: Project, file: PsiFile, edits: List<ArmsEdit>) {
        val document = file.viewProvider.document ?: return
        edits.sortedByDescending { it.startOffset }.forEach { edit ->
            val text = edit.plainText
            document.replaceString(edit.startOffset, edit.endOffset, text)
            PsiDocumentManager.getInstance(project).commitDocument(document)
            CodeStyleManager.getInstance(project).reformatText(file, edit.startOffset, edit.startOffset + text.length)
        }
    }

    fun startTemplate(project: Project, editor: Editor, templateText: String) {
        val templateManager = TemplateManager.getInstance(project)
        val template = templateManager.createTemplate("match-arms", "tolk", templateText)
        template.isToReformat = true
        templateManager.startTemplate(editor, template)
    }

    private class EditContext(
        private val document: Document,
        private val arms: List<TolkMatchArm>,
        private val lbraceEnd: Int,
    ) {
        fun edit(patterns: List<String>, anchorOffset: Int, withCaretMarker: Boolean = true): ArmsEdit {
            val previousArm = arms.lastOrNull { it.endOffset <= anchorOffset }
            val comma = if (previousArm != null && previousArm.needsTrailingComma()) "," else ""
            val startOffset = previousArm?.endOffset ?: lbraceEnd

            var armsText = armsTemplateText(patterns, withCaretMarker)
            if (!withCaretMarker) {
                // only the template edit reformats itself, so indent the rest like the arms around it
                val sibling = arms.lastOrNull()?.startOffset ?: anchorOffset
                armsText = armsText.indentLines(lineIndent(document, sibling))
            }

            if (!document.charsSequence.subSequence(startOffset, anchorOffset).isBlank()) {
                // a comment sits in between, keep it where it is and squeeze the arms in front of it
                return ArmsEdit(startOffset, startOffset, "$comma\n$armsText")
            }
            return ArmsEdit(startOffset, anchorOffset, "$comma\n$armsText\n" + lineIndent(document, anchorOffset))
        }
    }

    /**
     * Union variants already covered by the arms that are written down.
     *
     * A pattern doesn't have to spell a variant the way [render] does: an alias resolves to the same
     * type (`OkAlias<int>` for `Ok<int>`), and a generic type named without arguments stands for
     * every instantiation of it (`Ok` for `Ok<int>`), so patterns are matched as resolved types.
     */
    private class DeclaredVariants(arms: List<TolkMatchArm>, private val texts: Set<String>) {
        private val types = arms.mapNotNull { it.matchPattern.declaredType() }
        private val genericHeads = arms.mapNotNull { it.matchPattern.declarationWithoutTypeArguments() }

        fun covers(variant: TolkTy): Boolean {
            if (variant.render() in texts) return true
            if (types.any { it.isEquivalentTo(variant) }) return true
            val declaration = variant.declaration() ?: return false
            return genericHeads.any { it.manager.areElementsEquivalent(it, declaration) }
        }
    }

    private fun String.indentLines(indent: String): String =
        lineSequence().joinToString("\n") { if (it.isEmpty()) it else indent + it }

    /** Indentation of the line [offset] is on, or an empty string when something precedes it there. */
    private fun lineIndent(document: Document, offset: Int): String {
        val lineStart = document.getLineStartOffset(document.getLineNumber(offset))
        val prefix = document.getText(TextRange(lineStart, offset))
        return if (prefix.isBlank()) prefix else ""
    }

    private fun TolkTy.declaration(): PsiElement? = (unwrapTypeAlias() as? TolkTyPsiHolder)?.psi

    private fun TolkMatchPattern.declaredType(): TolkTy? {
        typeExpression?.let { return it.type }
        val reference = matchPatternReference ?: return null
        val symbol = reference.reference?.resolve() as? TolkTypeSymbolElement ?: return null
        val arguments = reference.typeArgumentList?.typeExpressionList
        if (arguments.isNullOrEmpty()) return symbol.type
        return when (symbol) {
            is TolkStruct -> TolkTyStruct.create(symbol, arguments)
            is TolkTypeDef -> TolkTyAlias.create(symbol, arguments)
            else -> symbol.type
        }
    }

    /** Declaration a pattern names without type arguments — it stands for every instantiation of it. */
    private fun TolkMatchPattern.declarationWithoutTypeArguments(): PsiElement? {
        val reference = matchPatternReference ?: return null
        if (reference.typeArgumentList != null) return null
        val symbol = reference.reference?.resolve() as? TolkTypeSymbolElement ?: return null
        return symbol.type.declaration()
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
