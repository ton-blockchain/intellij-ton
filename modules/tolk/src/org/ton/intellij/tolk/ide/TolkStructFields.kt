package org.ton.intellij.tolk.ide

import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.elementType
import com.intellij.psi.util.endOffset
import com.intellij.psi.util.startOffset
import org.ton.intellij.tolk.ide.configurable.tolkSettings
import org.ton.intellij.tolk.psi.TolkElementTypes
import org.ton.intellij.tolk.psi.TolkStructExpression
import org.ton.intellij.tolk.psi.TolkStructExpressionField
import org.ton.intellij.tolk.psi.TolkStructField
import org.ton.intellij.tolk.psi.impl.canUse
import org.ton.intellij.tolk.psi.impl.members
import org.ton.intellij.tolk.psi.impl.structFields
import org.ton.intellij.tolk.type.*

/**
 * Generation of struct initializer fields shared by the `Fill all fields…` completion
 * ([org.ton.intellij.tolk.ide.completion.TolkExpressionFieldProvider]) and the
 * [org.ton.intellij.tolk.ide.intentions.TolkFillStructFieldsIntention] intentions.
 */
object TolkStructFields {
    private const val VALUE_VARIABLE = "value"

    /**
     * Fields of the built struct that [structExpression] doesn't initialize yet, in declaration
     * order; only the ones without a default value when [requiredOnly] is set.
     *
     * Empty when the built type isn't a struct, or when some of its fields can't be used from here
     * — the generated initializer wouldn't be valid anyway.
     */
    fun missingFields(structExpression: TolkStructExpression, requiredOnly: Boolean = false): List<TolkStructField> {
        val structTy = structExpression.type?.actualType()?.unwrapTypeAlias() as? TolkTyStruct ?: return emptyList()
        val fields = structTy.psi.structFields
        if (fields.any { !it.canUse(structTy, structExpression) }) return emptyList()

        val initialized = structExpression.structExpressionBody.structExpressionFieldList
            .mapTo(hashSetOf()) { it.identifier.text.removeSurrounding("`") }

        return fields.filter { field ->
            val name = field.name ?: return@filter false
            name !in initialized && (!requiredOnly || field.expression == null)
        }
    }

    /** Renders [fields] as `name: value,` lines, each value being a template variable. */
    fun fieldsTemplateText(fields: List<TolkStructField>): String = fields
        .mapIndexed { index, field -> "${field.name}: \$$VALUE_VARIABLE$index\$," }
        .joinToString("\n")

    /** Default value of every variable produced by [fieldsTemplateText], by variable name. */
    fun fieldsVariables(fields: List<TolkStructField>, contextFile: PsiFile): Map<String, String> = fields
        .withIndex()
        .associate { (index, field) -> "$VALUE_VARIABLE$index" to typeDefaultValue(field.type, contextFile) }

    /** A single document replacement adding fields to a struct initializer. */
    data class FieldsEdit(
        val startOffset: Int,
        val endOffset: Int,
        val text: String,
        val variables: Map<String, String>,
    ) {
        /** [text] with the template variables resolved to their defaults, for a plain insertion. */
        val plainText: String
            get() = variables.entries.fold(text) { result, (name, value) -> result.replace("\$$name\$", value) }
    }

    /**
     * Where and what to insert to add [fields] to [structExpression].
     *
     * The fields land right where [caretOffset] is whenever the caret stands between the ones
     * already written down, and at the end of the initializer otherwise.
     */
    fun fieldsEdit(
        structExpression: TolkStructExpression,
        fields: List<TolkStructField>,
        caretOffset: Int,
    ): FieldsEdit? {
        if (fields.isEmpty()) return null
        val document = structExpression.containingFile?.viewProvider?.document ?: return null
        val body = structExpression.structExpressionBody
        val rbrace = body.rbrace ?: return null

        val initialized = body.structExpressionFieldList
        val caretAnchor = if (caretOffset in body.lbrace.endOffset..rbrace.startOffset) {
            initialized.firstOrNull { it.startOffset >= caretOffset }?.startOffset
        } else {
            null
        }
        val anchorOffset = caretAnchor ?: rbrace.startOffset

        val previousField = initialized.lastOrNull { it.endOffset <= anchorOffset }
        val previousComma = previousField?.trailingComma()
        // a field without its comma is only valid right before `}`, so inserting after it needs one
        val comma = if (previousField != null && previousComma == null) "," else ""
        val startOffset = previousComma?.endOffset ?: previousField?.endOffset ?: body.lbrace.endOffset

        val text = fieldsTemplateText(fields)
        val variables = fieldsVariables(fields, structExpression.containingFile)

        if (!document.charsSequence.subSequence(startOffset, anchorOffset).isBlank()) {
            // a comment sits in between, keep it where it is and squeeze the fields in front of it
            return FieldsEdit(startOffset, startOffset, "$comma\n$text", variables)
        }
        return FieldsEdit(
            startOffset,
            anchorOffset,
            "$comma\n$text\n" + lineIndent(document, anchorOffset),
            variables,
        )
    }

    /**
     * Applies [edit] as a live template, so that the caret lands on the first generated value and
     * tabs through the rest.
     */
    fun applyAsTemplate(project: Project, editor: Editor, edit: FieldsEdit) {
        val document = editor.document
        document.deleteString(edit.startOffset, edit.endOffset)
        PsiDocumentManager.getInstance(project).commitDocument(document)

        editor.caretModel.moveToOffset(edit.startOffset)
        startTemplate(project, editor, edit.text, edit.variables)
    }

    /** Applies [edit] as plain text — used to render the intention preview. */
    fun applyPlain(project: Project, file: PsiFile, edit: FieldsEdit) {
        val document = file.viewProvider.document ?: return
        val text = edit.plainText
        document.replaceString(edit.startOffset, edit.endOffset, text)
        PsiDocumentManager.getInstance(project).commitDocument(document)
        CodeStyleManager.getInstance(project).reformatText(file, edit.startOffset, edit.startOffset + text.length)
    }

    fun startTemplate(project: Project, editor: Editor, templateText: String, variables: Map<String, String>) {
        val templateManager = TemplateManager.getInstance(project)
        val template = templateManager.createTemplate("struct-fields", "tolk", templateText)
        template.isToReformat = true
        variables.forEach { (name, value) -> template.addVariable(name, ConstantNode(value), true) }
        templateManager.startTemplate(editor, template)
    }

    /** A value to prefill a field of [type] with, so that the initializer compiles as generated. */
    fun typeDefaultValue(type: TolkTy?, contextFile: PsiFile): String = when {
        type == null -> "null"
        type is TolkTyNull -> "null"
        type is TolkTyUnion && type.isNullable() -> "null"
        type is TolkTyBool -> "false"
        type is TolkTyCoins -> "${coinsFunctionName(contextFile)}(\"0.1\")"
        type is TolkIntTyFamily -> "0"
        type is TolkBitsNTy || type is TolkBytesNTy -> "createEmptySlice()"
        type is TolkTyAddress -> "address(\"\")"
        type is TolkCellTy -> "createEmptyCell()"
        type is TolkSliceTy -> "createEmptySlice()"
        type is TolkStringTy -> "\"\""
        type is TolkTyBuilder -> "beginCell()"
        type is TolkTyStruct -> structDefaultValue(type, contextFile)
        type is TolkTyEnum -> type.psi.members.firstOrNull()?.let { "${type.render()}.${it.name}" } ?: type.render()
        type is TolkTyArray -> "[]"
        type is TolkTyTypedTuple -> "[${type.elements.joinToString(", ") { typeDefaultValue(it, contextFile) }}]"
        type is TolkTyTensor -> "(${type.elements.joinToString(", ") { typeDefaultValue(it, contextFile) }})"
        type is TolkTyUnion -> typeDefaultValue(type.variants.first(), contextFile)
        else -> "null"
    }

    /** Use the generation site's stdlib, which can differ between projects in a monorepo. */
    private fun coinsFunctionName(contextFile: PsiFile): String {
        val stdlib = contextFile.originalFile.virtualFile?.let {
            contextFile.project.tolkSettings.getDefaultImport(it)
        }
        return when {
            stdlib?.resolveSymbols("grams", skipTypes = true)?.any() == true -> "grams"
            stdlib?.resolveSymbols("ton", skipTypes = true)?.any() == true -> "ton"
            else -> "grams"
        }
    }

    private fun structDefaultValue(type: TolkTyStruct, contextFile: PsiFile): String {
        if (type.psi.name == "map") return "[]"
        if (type.typeArguments.isNotEmpty() && type.psi.name == "Cell") {
            // `Cell<T>` -> `T {}.toCell()` or `defaultOf(T).toCell()`
            return "${typeDefaultValue(type.typeArguments[0], contextFile)}.toCell()"
        }
        return "${type.render()} {}"
    }

    /** Indentation of the line [offset] is on, or an empty string when something precedes it there. */
    private fun lineIndent(document: Document, offset: Int): String {
        val lineStart = document.getLineStartOffset(document.getLineNumber(offset))
        val prefix = document.getText(TextRange(lineStart, offset))
        return if (prefix.isBlank()) prefix else ""
    }

    /** The `,` closing this field — it belongs to the initializer body, not to the field itself. */
    private fun TolkStructExpressionField.trailingComma(): PsiElement? {
        var sibling: PsiElement? = nextSibling
        while (sibling is PsiWhiteSpace || sibling is PsiComment) {
            sibling = sibling.nextSibling
        }
        return sibling?.takeIf { it.elementType == TolkElementTypes.COMMA }
    }
}
