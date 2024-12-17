package org.ton.intellij.func.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import org.ton.intellij.func.psi.FuncApplyExpression
import org.ton.intellij.func.psi.FuncFunction
import org.ton.intellij.func.psi.FuncVisitor

class FuncImpureFunctionInspection : FuncInspectionBase() {
    override fun buildFuncVisitor(holder: ProblemsHolder, session: LocalInspectionToolSession): FuncVisitor {
        return object : FuncVisitor() {
            var currentFunction: FuncFunction? = null

            override fun visitFunction(o: FuncFunction) {
                currentFunction = o
                super.visitFunction(o)
            }

            override fun visitApplyExpression(o: FuncApplyExpression) {
                println("Apply: ${o.text} in ${currentFunction?.name}")
                super.visitApplyExpression(o)
            }
        }
    }
}
