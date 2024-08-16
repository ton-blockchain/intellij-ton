package org.ton.intellij.tolk.inspection.numeric

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.ton.intellij.tolk.eval.TolkConstantExpressionEvaluator
import org.ton.intellij.tolk.eval.TolkIntValue
import org.ton.intellij.tolk.inspection.TolkInspectionBase
import org.ton.intellij.tolk.psi.TolkBinExpression
import org.ton.intellij.tolk.psi.TolkExpression
import org.ton.intellij.tolk.psi.TolkVisitor
import org.ton.intellij.util.InspectionBundle
import java.math.BigInteger

class TolkDivideByZeroInspection : TolkInspectionBase() {
    override fun getGroupDisplayName(): String {
        return InspectionBundle.message("group.numeric.name")
    }

    override fun getDisplayName(): String {
        return InspectionBundle.message("inspection.numeric.divide_by_zero.name")
    }

    override fun buildTolkVisitor(holder: ProblemsHolder, session: LocalInspectionToolSession): TolkVisitor {
        return DivideByZeroVisitor(holder)
    }

    private inner class DivideByZeroVisitor(val holder: ProblemsHolder) : TolkVisitor() {
        override fun visitBinExpression(o: TolkBinExpression) {
            super.visitBinExpression(o)
            val binaryOp = o.binaryOp
            if (binaryOp.div == null &&
                binaryOp.divr == null &&
                binaryOp.divc == null &&
                binaryOp.mod == null &&
                binaryOp.modc == null &&
                binaryOp.modr == null &&
                binaryOp.divmod == null &&
                binaryOp.divlet == null &&
                binaryOp.divrlet == null &&
                binaryOp.divclet == null &&
                binaryOp.modlet == null &&
                binaryOp.modrlet == null &&
                binaryOp.modclet == null
            ) {
                return
            }
            if (o.right?.isZero(holder.project) ?: return) {
                holder.registerProblem(o, InspectionBundle.message("numeric.divide_by_zero"))
            }
        }
    }

    fun TolkExpression.isZero(project: Project): Boolean {
        val value = TolkConstantExpressionEvaluator.compute(project, this)
        return value is TolkIntValue && value.value == BigInteger.ZERO
    }
}
