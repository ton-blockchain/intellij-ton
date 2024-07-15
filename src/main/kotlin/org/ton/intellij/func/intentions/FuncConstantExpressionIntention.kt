package org.ton.intellij.func.intentions

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.ton.intellij.func.eval.FuncConstantExpressionEvaluator
import org.ton.intellij.func.eval.FuncIntValue
import org.ton.intellij.func.psi.FuncExpression
import org.ton.intellij.func.psi.FuncLiteralExpression
import org.ton.intellij.func.psi.FuncPsiFactory
import org.ton.intellij.util.InspectionBundle

class FuncConstantExpressionIntention : PsiElementBaseIntentionAction() {
    override fun getFamilyName(): String {
        return InspectionBundle.message("fix.constant.expression.family.name")
    }

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        if (element !is FuncExpression) return false
        if (element is FuncLiteralExpression && element.integerLiteral != null) return false
        val parent = element.parent
        if (parent is FuncExpression) return false
        return true
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val value = FuncConstantExpressionEvaluator.compute(project, element as? FuncExpression ?: return) ?: return
        val valueText = buildString {
            if (value is FuncIntValue && element is FuncLiteralExpression && element.stringLiteral != null) {
                append("0x")
                val hex = value.value.toString(16)
                append(hex)
            } else {
                append(value.toString())
            }
        }
        element.replace(FuncPsiFactory[project].createExpression(valueText))
    }
}
