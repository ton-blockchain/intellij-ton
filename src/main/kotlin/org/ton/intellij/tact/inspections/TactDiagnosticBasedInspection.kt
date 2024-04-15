package org.ton.intellij.tact.inspections

import com.intellij.codeInspection.ProblemsHolder
import org.ton.intellij.tact.psi.TactFunctionLike
import org.ton.intellij.tact.psi.TactVisitor
import org.ton.intellij.tact.type.selfInferenceResult

abstract class TactDiagnosticBasedInspection : TactLocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : TactVisitor() {
        override fun visitFunctionLike(o: TactFunctionLike) {
            o.selfInferenceResult.diagnostics.forEach {
                if (it.canApply(this@TactDiagnosticBasedInspection)) {
                    it.addToHolder(holder)
                }
            }
            super.visitFunctionLike(o)
        }
    }
}

class TactTypeCheckInspection : TactDiagnosticBasedInspection()
