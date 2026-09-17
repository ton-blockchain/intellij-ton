package org.ton.intellij.tolk.ide.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.ton.intellij.tolk.TolkBundle
import org.ton.intellij.tolk.ide.TolkStructFields
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.TolkStructExpression
import org.ton.intellij.tolk.psi.TolkStructExpressionField
import org.ton.intellij.tolk.psi.TolkStructField

abstract class TolkFillStructFieldsIntentionBase : PsiElementBaseIntentionAction() {
    override fun getFamilyName(): String = TolkBundle.message("intention.fill.struct.fields.family.name")

    /** Fields this intention writes out — empty when it has nothing to offer here. */
    protected abstract fun fieldsToFill(structExpression: TolkStructExpression): List<TolkStructField>

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        if (editor == null) return false
        val structExpression = findStructExpression(element) ?: return false
        return fieldsToFill(structExpression).isNotEmpty()
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        if (editor == null) return
        val edit = fieldsEdit(editor, element) ?: return
        TolkStructFields.applyAsTemplate(project, editor, edit)
    }

    override fun generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo {
        val element = file.findElementAt(editor.caretModel.offset) ?: return IntentionPreviewInfo.EMPTY
        val edit = fieldsEdit(editor, element) ?: return IntentionPreviewInfo.EMPTY
        TolkStructFields.applyPlain(project, file, edit)
        return IntentionPreviewInfo.DIFF
    }

    private fun fieldsEdit(editor: Editor, element: PsiElement): TolkStructFields.FieldsEdit? {
        val structExpression = findStructExpression(element) ?: return null
        val fields = fieldsToFill(structExpression)
        return TolkStructFields.fieldsEdit(structExpression, fields, editor.caretModel.offset)
    }

    /**
     * The innermost struct initializer the caret belongs to, unless the caret already sits inside
     * the value of one of its fields — there the user is editing that value, not the initializer.
     */
    private fun findStructExpression(element: PsiElement): TolkStructExpression? {
        var previous: PsiElement? = null
        var current: PsiElement? = element
        while (current != null && current !is TolkFile) {
            if (current is TolkStructExpressionField && previous === current.expression) return null
            if (current is TolkStructExpression) return current
            previous = current
            current = current.parent
        }
        return null
    }
}

class TolkFillStructFieldsIntention : TolkFillStructFieldsIntentionBase() {
    override fun getText(): String = TolkBundle.message("intention.fill.struct.fields.text")

    override fun fieldsToFill(structExpression: TolkStructExpression): List<TolkStructField> =
        TolkStructFields.missingFields(structExpression)
}

class TolkFillRequiredStructFieldsIntention :
    TolkFillStructFieldsIntentionBase(),
    LowPriorityAction {
    override fun getText(): String = TolkBundle.message("intention.fill.required.struct.fields.text")

    override fun fieldsToFill(structExpression: TolkStructExpression): List<TolkStructField> {
        val required = TolkStructFields.missingFields(structExpression, requiredOnly = true)
        // filling every field writes the same code, no point in offering both
        if (required.size == TolkStructFields.missingFields(structExpression).size) return emptyList()
        return required
    }
}
