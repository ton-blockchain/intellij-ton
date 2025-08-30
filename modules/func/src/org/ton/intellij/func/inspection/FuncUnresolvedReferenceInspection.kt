package org.ton.intellij.func.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import org.ton.intellij.func.ide.fixes.FuncCreateFunctionQuickfix
import org.ton.intellij.func.psi.*

class FuncUnresolvedReferenceInspection : FuncInspectionBase() {
    override fun buildFuncVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): FuncVisitor = object : FuncVisitor() {
        override fun visitReferenceExpression(o: FuncReferenceExpression) {
            super.visitReferenceExpression(o)
            val reference = o.reference ?: return
            val resolved = reference.resolve()
            if (resolved != null) return

            val id = o.identifier
            val range = TextRange.from(id.startOffsetInParent, id.textLength)

            val fixes = mutableListOf<FuncCreateFunctionQuickfix>()
            if (isFunctionCall(o)) {
                fixes.add(FuncCreateFunctionQuickfix(id))
            }

            holder.registerProblem(
                o,
                "Unresolved reference <code>#ref</code>",
                ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                range,
                *fixes.toTypedArray()
            )
        }

        private fun isFunctionCall(ref: FuncReferenceExpression): Boolean {
            return when (val parent = ref.parent) {
                is FuncApplyExpression        -> parent.left == ref
                is FuncSpecialApplyExpression -> {
                    val applyExpr = parent.left as? FuncApplyExpression
                    applyExpr?.left == ref
                }

                else                          -> false
            }
        }
    }
}
