package org.ton.intellij.tolk.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.ton.intellij.tolk.psi.TolkReferenceTypeExpression
import org.ton.intellij.tolk.psi.TolkVisitor
import org.ton.intellij.tolk.type.TolkType

class TolkUnresolvedTypeIdentifierInspection : TolkInspectionBase() {
    override fun buildTolkVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): TolkVisitor = object : TolkVisitor() {
        override fun visitReferenceTypeExpression(o: TolkReferenceTypeExpression) {
            val reference = o.reference ?: return
            if (o.type is TolkType.GenericType) return
            if (reference.resolve() == null) {
                val id = o.identifier
                holder.registerProblem(
                    o,
                    "Unresolved type identifier <code>#ref</code>",
                    ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                    id.textRangeInParent
                )
            }
        }
    }
}
