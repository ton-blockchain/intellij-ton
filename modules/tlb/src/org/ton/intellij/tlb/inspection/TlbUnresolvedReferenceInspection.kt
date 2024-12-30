package org.ton.intellij.tlb.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.ton.intellij.tlb.psi.TlbParamTypeExpression
import org.ton.intellij.tlb.psi.TlbReference
import org.ton.intellij.tlb.psi.TlbVisitor

class TlbUnresolvedReferenceInspection : TlbInspectionBase() {
    override fun buildTlbVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession
    ): TlbVisitor = object : TlbVisitor() {
        override fun visitParamTypeExpression(expression: TlbParamTypeExpression) {
            val name = expression.identifier?.text ?: return
            if (name == "Any" || name == "Cell" || name.startsWith("uint") || name.startsWith("int") || name.startsWith("bits")) return
            val reference = expression.reference as? TlbReference ?: return
            if (reference.multiResolve(false).isEmpty()) {
                holder.registerProblem(expression, "Unresolved reference: ${expression.text}", ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
            }
        }
    }
}