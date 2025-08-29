package org.ton.intellij.func.inspection.style

import com.intellij.codeInspection.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.ton.intellij.func.FuncBundle
import org.ton.intellij.func.inspection.FuncInspectionBase
import org.ton.intellij.func.psi.*

class FuncOperatorPrecedenceInspection : FuncInspectionBase() {
    override fun buildFuncVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): FuncVisitor = object : FuncVisitor() {
        override fun visitBinExpression(expression: FuncBinExpression) {
            checkSuspiciousPrecedence(expression, holder)
        }
    }

    private fun checkSuspiciousPrecedence(expression: FuncBinExpression, holder: ProblemsHolder) {
        if (isSuspiciousMixedPrecedence(expression)) {
            holder.registerProblem(
                expression,
                FuncBundle.message("inspection.func.operator.precedence.message"),
                ProblemHighlightType.WEAK_WARNING,
                FuncAddParenthesesFix(expression)
            )
        }
    }

    private fun isSuspiciousMixedPrecedence(expression: FuncBinExpression): Boolean {
        val operator = expression.binaryOp.text ?: return false

        // Focus on logical operators (|, &, ^) mixed with comparison operators
        if (operator in setOf("|", "&", "^")) {
            // Check if we have comparison operators at the same level without parentheses
            val left = expression.left
            val right = expression.right

            return (hasComparisonAtTopLevel(left) || hasComparisonAtTopLevel(right))
        }

        // Also check for comparison operators that might have logical operators mixed in
        if (operator in setOf("<", ">", "<=", ">=", "==", "!=")) {
            val left = expression.left
            val right = expression.right

            return (hasLogicalAtTopLevel(left) || hasLogicalAtTopLevel(right))
        }

        return false
    }
}

class FuncAddParenthesesFix(
    element: PsiElement,
) : LocalQuickFixAndIntentionActionOnPsiElement(element), LocalQuickFix {

    override fun getFamilyName(): String = FuncBundle.message("inspection.func.operator.precedence.fix.family.name")
    override fun getText(): String = FuncBundle.message("inspection.func.operator.precedence.fix.text")

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement,
    ) {
        val expression = startElement as? FuncBinExpression ?: return
        addParenthesesForClarity(project, expression)
    }

    private fun addParenthesesForClarity(project: Project, expression: FuncBinExpression) {
        val factory = FuncPsiFactory[project]
        val operator = expression.binaryOp.text ?: return
        val left = expression.left
        val right = expression.right ?: return

        if (operator in setOf("|", "&", "^")) {
            // For logical operators, wrap comparison subexpressions in parentheses
            val newLeftText = if (hasComparisonAtTopLevel(left)) "(${left.text})" else left.text
            val newRightText = if (hasComparisonAtTopLevel(right)) "(${right.text})" else right.text

            val newExpressionText = "$newLeftText $operator $newRightText"
            val newExpression = factory.createExpression(newExpressionText)
            expression.replace(newExpression)
        } else if (operator in setOf("<", ">", "<=", ">=", "==", "!=")) {
            // For comparison operators, wrap the entire expression in parentheses if it contains logical ops
            val newLeftText = if (hasLogicalAtTopLevel(left)) "(${left.text})" else left.text
            val newRightText = if (hasLogicalAtTopLevel(right)) "(${right.text})" else right.text

            val newExpressionText = "$newLeftText $operator $newRightText"
            val newExpression = factory.createExpression(newExpressionText)
            expression.replace(newExpression)
        }
    }
}


private fun hasComparisonAtTopLevel(expr: FuncExpression?): Boolean {
    if (expr !is FuncBinExpression) return false
    val op = expr.binaryOp.text ?: return false
    return op in setOf("<", ">", "<=", ">=", "==", "!=")
}

private fun hasLogicalAtTopLevel(expr: FuncExpression?): Boolean {
    if (expr !is FuncBinExpression) return false
    val op = expr.binaryOp.text ?: return false
    return op in setOf("|", "&", "^")
}