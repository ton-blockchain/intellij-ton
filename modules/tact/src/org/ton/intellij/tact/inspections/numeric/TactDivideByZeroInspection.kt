package org.ton.intellij.tact.inspections.numeric

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.ton.intellij.tact.eval.TactConstantExpressionEvaluator
import org.ton.intellij.tact.eval.TactIntValue
import org.ton.intellij.tact.inspections.TactLocalInspectionTool
import org.ton.intellij.tact.psi.TactBinExpression
import org.ton.intellij.tact.psi.TactExpression
import org.ton.intellij.tact.psi.TactVisitor
import org.ton.intellij.util.InspectionBundle
import java.math.BigInteger

class TactDivideByZeroInspection : TactLocalInspectionTool() {
    override fun getGroupDisplayName(): String {
        return InspectionBundle.message("group.numeric.name")
    }

    override fun getDisplayName(): String {
        return InspectionBundle.message("inspection.numeric.divide_by_zero.name")
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): TactVisitor = DivideByZeroVisitor(holder)

    private inner class DivideByZeroVisitor(
        val holder: ProblemsHolder
    ) : TactVisitor() {
        override fun visitBinExpression(o: TactBinExpression) {
            super.visitBinExpression(o)
            val binaryOp = o.binOp
            if (binaryOp.div == null) {
                return
            }
            if (o.right?.isZero(holder.project) == true) {
                holder.registerProblem(o, InspectionBundle.message("numeric.divide_by_zero"))
            }
        }
    }

    private fun TactExpression.isZero(project: Project): Boolean {
        val value = TactConstantExpressionEvaluator.compute(project, this)
        return value is TactIntValue && value.value == BigInteger.ZERO
    }
}
