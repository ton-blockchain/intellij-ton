package org.ton.intellij.func.inspection.numeric

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import org.ton.intellij.func.eval.FuncConstantExpressionEvaluator
import org.ton.intellij.func.inspection.FuncInspectionBase
import org.ton.intellij.func.psi.FuncExpression
import org.ton.intellij.func.psi.FuncVisitor
import org.ton.intellij.util.InspectionBundle
import org.ton.intellij.util.exception.ConstantEvaluationOverflowException

private val HAS_OVERFLOW_IN_CHILD = Key.create<Unit>("HAS_OVERFLOW_IN_CHILD")

class FuncIntOverflowInspection : FuncInspectionBase() {
    override fun getGroupDisplayName(): String {
        return InspectionBundle.message("group.numeric.name")
    }

    override fun getDisplayName(): String {
        return InspectionBundle.message("inspection.numeric.integer_overflow.name")
    }

    override fun buildFuncVisitor(holder: ProblemsHolder, session: LocalInspectionToolSession): FuncVisitor {
        return IntOverflowInspectionVisitor(holder)
    }

    private inner class IntOverflowInspectionVisitor(val holder: ProblemsHolder) : FuncVisitor() {
        override fun visitExpression(o: FuncExpression) {
            if (hasOverflow(o, holder.project)) {
                holder.registerProblem(o, InspectionBundle.message("numeric.integer_overflow"))
            }
        }
    }

    private fun hasOverflow(expression: FuncExpression, project: Project): Boolean {
        var result = false
        var toStoreInParent = false
        try {
            if (expression.getUserData(HAS_OVERFLOW_IN_CHILD) == null) {
                FuncConstantExpressionEvaluator.compute(project, expression, true)
            } else {
                toStoreInParent = true
                expression.putUserData(HAS_OVERFLOW_IN_CHILD, Unit)
            }
        } catch (e: ConstantEvaluationOverflowException) {
            result = true
            toStoreInParent = true
        } finally {
            val parent = expression.parent
            if (toStoreInParent && parent is FuncExpression) {
                parent.putUserData(HAS_OVERFLOW_IN_CHILD, Unit)
            }
        }
        return result
    }
}
