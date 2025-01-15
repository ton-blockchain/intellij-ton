package org.ton.intellij.tact.inspections.style

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import org.ton.intellij.tact.eval.TactConstantExpressionEvaluator
import org.ton.intellij.tact.inspections.TactLocalInspectionTool
import org.ton.intellij.tact.psi.TactExpression
import org.ton.intellij.tact.psi.TactIntegerExpression
import org.ton.intellij.tact.psi.TactPsiFactory
import org.ton.intellij.tact.psi.TactVisitor
import org.ton.intellij.util.InspectionBundle

private const val MAX_RESULT_LENGTH_TO_DISPLAY = 40

class TactConstantExpressionInspection : TactLocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        ComputeConstantVisitor(holder)

    private class ComputeConstantVisitor(
        val holder: ProblemsHolder
    ) : TactVisitor() {
        override fun visitExpression(expression: TactExpression) {
            if (expression is TactIntegerExpression) return
            val parent = expression.parent
            if (parent is TactExpression) return
            val value = TactConstantExpressionEvaluator.compute(holder.project, expression) ?: return
            val valueText = value.toString()
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
                TactComputeConstantExpressionFix(expression, valueText)
            )
        }
    }

    private class TactComputeConstantExpressionFix(
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
            startElement.replace(TactPsiFactory[project].createExpression(valueText))
        }
    }
}
