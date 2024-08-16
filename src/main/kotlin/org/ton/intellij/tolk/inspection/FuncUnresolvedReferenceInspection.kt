package org.ton.intellij.tolk.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import org.ton.intellij.tolk.psi.TolkReferenceExpression
import org.ton.intellij.tolk.psi.TolkVisitor

class TolkUnresolvedReferenceInspection : TolkInspectionBase() {
    override fun buildTolkVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): TolkVisitor = object : TolkVisitor() {
        override fun visitReferenceExpression(o: TolkReferenceExpression) {
            super.visitReferenceExpression(o)
            val reference = o.reference ?: return
            val resolved = reference.resolve()
            if (resolved != null) return

            val id = o.identifier
            val range = TextRange.from(id.startOffsetInParent, id.textLength)
            holder.registerProblem(
                o,
                "Unresolved reference <code>#ref</code>",
                ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                range
            )
        }
    }
}
