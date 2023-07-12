package org.ton.intellij.func.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import org.ton.intellij.func.psi.FuncReferenceExpression
import org.ton.intellij.func.psi.FuncVisitor
import org.ton.intellij.func.psi.impl.FuncReferenceExpressionImpl

class FuncUnresolvedReferenceInspection : FuncInspectionBase() {
    override fun buildFuncVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): FuncVisitor = object : FuncVisitor() {
        override fun visitReferenceExpression(o: FuncReferenceExpression) {
            super.visitReferenceExpression(o)
            if (o !is FuncReferenceExpressionImpl) return
            val reference = o.reference ?: return

            if (reference.resolve() == null) {
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
}
