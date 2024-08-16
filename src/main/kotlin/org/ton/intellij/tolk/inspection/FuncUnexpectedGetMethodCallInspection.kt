package org.ton.intellij.tolk.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkReferenceExpression
import org.ton.intellij.tolk.psi.TolkVisitor
import org.ton.intellij.tolk.psi.impl.TolkReferenceExpressionImpl
import org.ton.intellij.tolk.psi.impl.hasMethodId

class TolkUnexpectedGetMethodCallInspection : TolkInspectionBase() {
    override fun buildTolkVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): TolkVisitor = object : TolkVisitor() {
        override fun visitReferenceExpression(o: TolkReferenceExpression) {
            super.visitReferenceExpression(o)
            if (o !is TolkReferenceExpressionImpl) return
            val reference = o.reference ?: return
            val resolvedFunction = reference.resolve() as? TolkFunction ?: return

            if (resolvedFunction.hasMethodId) {
                val id = o.identifier
                val range = TextRange.from(id.startOffsetInParent, id.textLength)
                // Suspend function 'a' should be called only from a coroutine or another suspend function
                holder.registerProblem(
                    o,
                    "Method-id function <code>#ref</code> should not be called from other functions",
                    ProblemHighlightType.GENERIC_ERROR,
                    range
                )
            }
        }
    }
}
