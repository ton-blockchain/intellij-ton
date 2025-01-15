package org.ton.intellij.tact.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.ton.intellij.tact.psi.TactFile

abstract class TactLocalInspectionTool : LocalInspectionTool() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ): PsiElementVisitor {
        val file = session.file
        return if (file is TactFile && isApplicableTo(file)) {
            buildVisitor(holder, isOnTheFly)
        } else {
            PsiElementVisitor.EMPTY_VISITOR
        }
    }

    protected fun isApplicableTo(file: TactFile): Boolean {
        return true
    }
}
