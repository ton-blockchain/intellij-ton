package org.ton.intellij.tolk.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import org.ton.intellij.tolk.psi.TolkTypeIdentifier
import org.ton.intellij.tolk.psi.TolkVisitor
import org.ton.intellij.tolk.psi.impl.TolkTypeIdentifierImpl

class TolkUnresolvedTypeIdentifierInspection : TolkInspectionBase() {
    override fun buildTolkVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): TolkVisitor = object : TolkVisitor() {
        override fun visitTypeIdentifier(o: TolkTypeIdentifier) {
            super.visitTypeIdentifier(o)
            if (o !is TolkTypeIdentifierImpl) return
            val reference = o.reference ?: return
            if (reference.resolve() == null) {
                val id = o.identifier
                val range = TextRange.from(id.startOffsetInParent, id.textLength)
                holder.registerProblem(
                    o,
                    "Unresolved type identifier <code>#ref</code>",
                    ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                    range
                )
            }
        }
    }
}
