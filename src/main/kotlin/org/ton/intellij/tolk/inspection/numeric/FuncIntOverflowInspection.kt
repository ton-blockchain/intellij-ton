package org.ton.intellij.tolk.inspection.numeric

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import org.ton.intellij.tolk.eval.TolkConstantExpressionEvaluator
import org.ton.intellij.tolk.inspection.TolkInspectionBase
import org.ton.intellij.tolk.psi.TolkExpression
import org.ton.intellij.tolk.psi.TolkVisitor
import org.ton.intellij.util.InspectionBundle
import org.ton.intellij.util.exception.ConstantEvaluationOverflowException

private val HAS_OVERFLOW_IN_CHILD = Key.create<Unit>("HAS_OVERFLOW_IN_CHILD")

class TolkIntOverflowInspection : TolkInspectionBase() {
    override fun getGroupDisplayName(): String {
        return InspectionBundle.message("group.numeric.name")
    }

    override fun getDisplayName(): String {
        return InspectionBundle.message("inspection.numeric.integer_overflow.name")
    }

    override fun buildTolkVisitor(holder: ProblemsHolder, session: LocalInspectionToolSession): TolkVisitor {
        return IntOverflowInspectionVisitor(holder)
    }

    private inner class IntOverflowInspectionVisitor(val holder: ProblemsHolder) : TolkVisitor() {
        override fun visitExpression(o: TolkExpression) {
            if (hasOverflow(o, holder.project)) {
                holder.registerProblem(o, InspectionBundle.message("numeric.integer_overflow"))
            }
        }
    }

    private fun hasOverflow(expression: TolkExpression, project: Project): Boolean {
        var result = false
        var toStoreInParent = false
        try {
            if (expression.getUserData(HAS_OVERFLOW_IN_CHILD) == null) {
                TolkConstantExpressionEvaluator.compute(project, expression, true)
            } else {
                toStoreInParent = true
                expression.putUserData(HAS_OVERFLOW_IN_CHILD, Unit)
            }
        } catch (e: ConstantEvaluationOverflowException) {
            result = true
            toStoreInParent = true
        } finally {
            val parent = expression.parent
            if (toStoreInParent && parent is TolkExpression) {
                parent.putUserData(HAS_OVERFLOW_IN_CHILD, Unit)
            }
        }
        return result
    }
}
