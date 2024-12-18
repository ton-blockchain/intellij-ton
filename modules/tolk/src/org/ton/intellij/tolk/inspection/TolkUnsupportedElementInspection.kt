package org.ton.intellij.tolk.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.ton.intellij.tolk.TolkBundle
import org.ton.intellij.tolk.psi.*

class TolkUnsupportedElementInspection : TolkInspectionBase() {
    override fun buildTolkVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): TolkVisitor = object : TolkVisitor() {
        override fun visitUnionType(o: TolkUnionType) {
            holder.registerProblem(
                o,
                TolkBundle.message("inspection.unsupported_element.description"),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            )
        }

        override fun visitAsExpression(o: TolkAsExpression) {
            reportProblem(o)
        }

        override fun visitTypeArgumentList(o: TolkTypeArgumentList) {
            reportProblem(o)
        }

        override fun visitTypeDef(o: TolkTypeDef) {
            reportProblem(o)
        }

        override fun visitStruct(o: TolkStruct) {
            reportProblem(o)
        }

        fun reportProblem(psiElement: TolkElement) {
            holder.registerProblem(
                psiElement,
                TolkBundle.message("inspection.unsupported_element.description"),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            )
        }
    }
}