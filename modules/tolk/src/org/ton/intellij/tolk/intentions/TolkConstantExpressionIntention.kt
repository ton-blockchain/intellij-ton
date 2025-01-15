package org.ton.intellij.tolk.intentions

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.eval.TolkConstantExpressionEvaluator
import org.ton.intellij.tolk.eval.TolkIntValue
import org.ton.intellij.tolk.psi.TolkExpression
import org.ton.intellij.tolk.psi.TolkLiteralExpression
import org.ton.intellij.tolk.psi.TolkPsiFactory
import org.ton.intellij.util.InspectionBundle

class TolkConstantExpressionIntention : PsiElementBaseIntentionAction() {
    override fun getFamilyName(): String {
        return InspectionBundle.message("fix.constant.expression.family.name")
    }

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        if (element !is TolkExpression) return false
        if (element is TolkLiteralExpression && element.integerLiteral != null) return false
        val parent = element.parent
        return parent !is TolkExpression
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val value = TolkConstantExpressionEvaluator.compute(project, element as? TolkExpression ?: return) ?: return
        val valueText = buildString {
            if (value is TolkIntValue && element is TolkLiteralExpression && element.stringLiteral != null) {
                append("0x")
                val hex = value.value.toString(16)
                append(hex)
            } else {
                append(value.toString())
            }
        }
        element.replace(TolkPsiFactory[project].createExpression(valueText))
    }
}
