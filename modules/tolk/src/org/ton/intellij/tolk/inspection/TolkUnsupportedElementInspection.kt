package org.ton.intellij.tolk.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.ton.intellij.tolk.TolkBundle
import org.ton.intellij.tolk.psi.TolkStruct
import org.ton.intellij.tolk.psi.TolkTypeDef
import org.ton.intellij.tolk.psi.TolkUnionType
import org.ton.intellij.tolk.psi.TolkVisitor

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

        override fun visitTypeDef(o: TolkTypeDef) {
            holder.registerProblem(
                o,
                TolkBundle.message("inspection.unsupported_element.description"),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            )
        }

        override fun visitStruct(o: TolkStruct) {
            holder.registerProblem(
                o,
                TolkBundle.message("inspection.unsupported_element.description"),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            )
        }
    }
}