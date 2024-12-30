package org.ton.intellij.tlb.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import org.ton.intellij.tlb.psi.TlbCommonField
import org.ton.intellij.tlb.psi.TlbParamTypeExpression
import org.ton.intellij.tlb.psi.TlbVisitor
import org.ton.intellij.tlb.psi.isNatural

class TlbFieldAsExpressionInspection : TlbInspectionBase() {
    override fun buildTlbVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession
    ): TlbVisitor = object : TlbVisitor() {
        override fun visitParamTypeExpression(o: TlbParamTypeExpression) {
            val resolved = o.reference?.resolve()
            if (resolved is TlbCommonField && !resolved.typeExpression.isNatural()) {
                holder.registerProblem(
                    o,
                    "Can't use a field in an expression unless it is either an integer or a type",
                )
            }
        }
    }
}