package org.ton.intellij.func.inspection.numeric

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.ton.intellij.func.eval.FuncConstantExpressionEvaluator
import org.ton.intellij.func.eval.FuncIntValue
import org.ton.intellij.func.inspection.FuncInspectionBase
import org.ton.intellij.func.psi.FuncBinExpression
import org.ton.intellij.func.psi.FuncExpression
import org.ton.intellij.func.psi.FuncVisitor
import org.ton.intellij.util.InspectionBundle
import java.math.BigInteger

class FuncDivideByZeroInspection : FuncInspectionBase() {
    override fun getGroupDisplayName(): String {
        return InspectionBundle.message("group.numeric.name")
    }

    override fun getDisplayName(): String {
        return InspectionBundle.message("inspection.numeric.divide_by_zero.name")
    }

    override fun buildFuncVisitor(holder: ProblemsHolder, session: LocalInspectionToolSession): FuncVisitor {
        return DivideByZeroVisitor(holder)
    }

    private inner class DivideByZeroVisitor(val holder: ProblemsHolder) : FuncVisitor() {
        override fun visitBinExpression(o: FuncBinExpression) {
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

    fun FuncExpression.isZero(project: Project): Boolean {
        val value = FuncConstantExpressionEvaluator.compute(project, this)
        return value is FuncIntValue && value.value == BigInteger.ZERO
    }
}
