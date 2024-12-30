package org.ton.intellij.tlb.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import org.ton.intellij.tlb.psi.TlbGetBitTypeExpression
import org.ton.intellij.tlb.psi.TlbVisitor
import org.ton.intellij.tlb.psi.isNegated
import org.ton.intellij.tlb.psi.unwrap

class TlbGetBitNegateInspection : TlbInspectionBase(){
    override fun buildTlbVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession
    ): TlbVisitor = object : TlbVisitor() {
        override fun visitGetBitTypeExpression(o: TlbGetBitTypeExpression) {
            val typeExpressionList = o.typeExpressionList
            val left = typeExpressionList.firstOrNull()?.unwrap() ?: return
            if (left.isNegated()) {
                holder.registerProblem(
                    left,
                    "Can't apply bit selection operator `.` to values of negative polarity",
                )
            }

            val right = typeExpressionList.getOrNull(1)?.unwrap() ?: return
            if (right.isNegated()) {
                holder.registerProblem(
                    right,
                    "Can't apply bit selection operator `.` to values of negative polarity",
                )
            }
        }
    }
}