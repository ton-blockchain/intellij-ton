package org.ton.intellij.tolk.inspection.style

import com.intellij.codeInspection.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.ton.intellij.tolk.eval.TolkConstantExpressionEvaluator
import org.ton.intellij.tolk.eval.TolkIntValue
import org.ton.intellij.tolk.inspection.TolkInspectionBase
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.util.InspectionBundle

private const val MAX_RESULT_LENGTH_TO_DISPLAY = 40

class TolkConstantExpressionInspection : TolkInspectionBase() {
    override fun buildTolkVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): TolkVisitor = object : TolkVisitor() {
        override fun visitExpression(expression: TolkExpression) {
            val parent = expression.parent
            if (expression is TolkLiteralExpression && expression.integerLiteral != null) return
            if (parent is TolkBinExpression && (parent.binaryOp.eq == null || parent.right != expression)) return
            if (parent is TolkExpression && parent !is TolkBinExpression) return
            val value = TolkConstantExpressionEvaluator.compute(holder.project, expression) ?: return
            if (value !is TolkIntValue) return
            val valueText = buildString {
                if (expression is TolkLiteralExpression && expression.stringLiteral != null) {
                    append("0x")
                    val hex = value.value.toString(16)
                    append(hex)
                } else {
                    append(value.toString())
                }
            }
            val message = if (valueText.length > MAX_RESULT_LENGTH_TO_DISPLAY) {
                InspectionBundle.message("fix.constant.expression.name.short")
            } else {
                InspectionBundle.message("fix.constant.expression.name", valueText)
            }
            val range = TextRange.from(expression.startOffsetInParent, expression.textLength)
            holder.registerProblem(
                parent,
                message,
                ProblemHighlightType.INFORMATION,
                range,
                TolkComputeConstantExpressionFix(expression, valueText)
            )
        }
    }
}

class TolkComputeConstantExpressionFix(
    element: PsiElement,
    private val valueText: String
) : LocalQuickFixAndIntentionActionOnPsiElement(element), LocalQuickFix {
    override fun getFamilyName(): String =
        InspectionBundle.message("fix.constant.expression.family.name")

    override fun getText(): String {
        return if (valueText.length < MAX_RESULT_LENGTH_TO_DISPLAY) {
            InspectionBundle.message("fix.constant.expression.name", valueText)
        } else {
            InspectionBundle.message("fix.constant.expression.name.short")
        }
    }

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement
    ) {
        startElement.replace(TolkPsiFactory[project].createExpression(valueText))
    }
}
