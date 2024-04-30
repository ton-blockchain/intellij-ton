package org.ton.intellij.tact.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.ton.intellij.tact.TactBundle
import org.ton.intellij.tact.psi.TactCallExpression
import org.ton.intellij.tact.psi.TactReferenceExpression
import org.ton.intellij.tact.psi.TactVisitor

class TactUnresolvedReferenceInspection : TactLocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = object : TactVisitor() {
        override fun visitReferenceExpression(o: TactReferenceExpression) {
            val reference = o.reference ?: return
            reference.resolve() ?: return holder.registerProblem(o)
        }

        override fun visitCallExpression(o: TactCallExpression) {
            val reference = o.reference ?: return
            reference.resolve() ?: return holder.registerProblem(o)
        }
    }

    private fun ProblemsHolder.registerProblem(
        element: TactReferenceExpression,
    ) {
        val referenceName = element.identifier.text
        val description = TactBundle.message("inspection.message.unresolved.reference", referenceName)
        val highlightedElement = element.identifier
        registerProblem(highlightedElement, description, ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
    }

    private fun ProblemsHolder.registerProblem(
        element: TactCallExpression,
    ) {
        val referenceName = element.identifier.text
        val description = TactBundle.message("inspection.message.unresolved.reference", referenceName)
        val highlightedElement = element.identifier
        registerProblem(highlightedElement, description, ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
    }
}
