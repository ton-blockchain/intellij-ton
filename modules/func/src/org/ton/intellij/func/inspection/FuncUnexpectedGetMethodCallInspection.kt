package org.ton.intellij.func.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import org.ton.intellij.func.psi.FuncFunction
import org.ton.intellij.func.psi.FuncReferenceExpression
import org.ton.intellij.func.psi.FuncVisitor
import org.ton.intellij.func.psi.impl.FuncReferenceExpressionImpl
import org.ton.intellij.func.psi.impl.hasMethodId

class FuncUnexpectedGetMethodCallInspection : FuncInspectionBase() {
    override fun buildFuncVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): FuncVisitor = object : FuncVisitor() {
        override fun visitReferenceExpression(o: FuncReferenceExpression) {
            super.visitReferenceExpression(o)
            if (o !is FuncReferenceExpressionImpl) return
            val reference = o.reference ?: return
            val resolvedFunction = reference.resolve() as? FuncFunction ?: return

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
