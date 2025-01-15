package org.ton.intellij.tlb.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.util.parentOfType
import org.ton.intellij.tlb.psi.TlbCommonField
import org.ton.intellij.tlb.psi.TlbNegatedTypeExpression
import org.ton.intellij.tlb.psi.TlbParamList
import org.ton.intellij.tlb.psi.TlbVisitor

class TlbNegateExplicitFieldInspection : TlbInspectionBase() {
    override fun buildTlbVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession
    ): TlbVisitor = object : TlbVisitor() {
        override fun visitNegatedTypeExpression(o: TlbNegatedTypeExpression) {
            val typeExpression = o.typeExpression
            val resolved = typeExpression?.reference?.resolve()
            if (resolved is TlbCommonField && o.parentOfType<TlbParamList>() == null) {
                holder.registerProblem(
                    typeExpression,
                    "Can't negate an explicit field",
                )
            }
        }
    }
}