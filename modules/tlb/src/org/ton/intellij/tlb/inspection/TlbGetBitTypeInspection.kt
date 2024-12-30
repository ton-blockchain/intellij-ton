package org.ton.intellij.tlb.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import org.ton.intellij.tlb.psi.*

class TlbGetBitTypeInspection : TlbInspectionBase() {
    override fun buildTlbVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession
    ): TlbVisitor = object : TlbVisitor() {
        override fun visitGetBitTypeExpression(o: TlbGetBitTypeExpression) {
            val typeExpressionList = o.typeExpressionList
            val left = typeExpressionList.firstOrNull()?.unwrap() ?: return
            val resolvedParam = left.reference?.resolve()
            when (resolvedParam) {
                is TlbImplicitField -> {
                    if (resolvedParam.typeKeyword != null) {
                        reportProblem(holder, left)
                    }
                }

                is TlbConstructor,
                is TlbResultType -> {
                    reportProblem(holder, left)
                }
            }
        }
    }

    private fun reportProblem(holder: ProblemsHolder, element: PsiElement) {
        holder.registerProblem(
            element,
            "Can't apply bit selection operator `.` to types",
        )
    }
}