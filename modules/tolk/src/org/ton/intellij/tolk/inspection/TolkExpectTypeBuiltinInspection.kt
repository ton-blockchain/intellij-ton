package org.ton.intellij.tolk.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import org.ton.intellij.tolk.eval.TolkSliceValue
import org.ton.intellij.tolk.eval.value
import org.ton.intellij.tolk.psi.TolkCallExpression
import org.ton.intellij.tolk.psi.TolkLiteralExpression
import org.ton.intellij.tolk.psi.TolkReferenceExpression
import org.ton.intellij.tolk.psi.TolkVisitor

class TolkExpectTypeBuiltinInspection  : TolkInspectionBase() {
    override fun buildTolkVisitor(holder: ProblemsHolder, session: LocalInspectionToolSession): TolkVisitor = object  : TolkVisitor() {
        override fun visitCallExpression(o: TolkCallExpression) {
            val referenceExpr = o.expression as? TolkReferenceExpression ?: return
            val name = referenceExpr.name ?: return
            if (name != "__expect_type") return
            val argumentList = o.argumentList.argumentList
            val left = argumentList.firstOrNull() ?: return
            val right = argumentList.getOrNull(1) ?: return
            val expectTypeText = ((right.expression as? TolkLiteralExpression)?.value as? TolkSliceValue)?.value ?: return
            val actualType = left.expression.type ?: return

            val actualTypeText = buildString {
                actualType.printDisplayName(this)
            }
            if (expectTypeText != actualTypeText) {
                holder.registerProblem(
                    left,
                    "Type mismatch\nexpected: `$expectTypeText`, but found: `$actualTypeText`\nactual: `$actualType`",
                )
            }
        }
    }
}
