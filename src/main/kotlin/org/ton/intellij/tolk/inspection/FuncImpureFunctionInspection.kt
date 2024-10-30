package org.ton.intellij.tolk.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkVisitor

class TolkImpureFunctionInspection : TolkInspectionBase() {
    override fun buildTolkVisitor(holder: ProblemsHolder, session: LocalInspectionToolSession): TolkVisitor {
        return object : TolkVisitor() {
            var currentFunction: TolkFunction? = null

            override fun visitFunction(o: TolkFunction) {
                currentFunction = o
                super.visitFunction(o)
            }


        }
    }
}
