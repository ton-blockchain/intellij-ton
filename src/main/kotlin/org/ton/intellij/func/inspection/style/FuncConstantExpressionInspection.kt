package org.ton.intellij.func.inspection.style

import com.intellij.codeInspection.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.ton.intellij.func.eval.FuncConstantExpressionEvaluator
import org.ton.intellij.func.eval.FuncIntValue
import org.ton.intellij.func.inspection.FuncInspectionBase
import org.ton.intellij.func.psi.*
import org.ton.intellij.util.InspectionBundle

private const val MAX_RESULT_LENGTH_TO_DISPLAY = 40

class FuncConstantExpressionInspection : FuncInspectionBase() {
    override fun buildFuncVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): FuncVisitor = object : FuncVisitor() {
        override fun visitExpression(expression: FuncExpression) {
            val parent = expression.parent
            if (expression is FuncLiteralExpression && expression.integerLiteral != null) return
            if (parent is FuncBinExpression && (parent.binaryOp.eq == null || parent.right != expression)) return
            if (parent is FuncExpression && parent !is FuncBinExpression) return
            val value = FuncConstantExpressionEvaluator.compute(holder.project, expression) ?: return
            val valueText = buildString {
                if (value is FuncIntValue && expression is FuncLiteralExpression && expression.stringLiteral != null) {
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
                FuncComputeConstantExpressionFix(expression, valueText)
            )
        }
    }
}

class FuncComputeConstantExpressionFix(
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
        startElement.replace(FuncPsiFactory[project].createExpression(valueText))
    }
}
