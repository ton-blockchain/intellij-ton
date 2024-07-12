package org.ton.intellij.func.diagnostics

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import org.ton.intellij.func.inspection.FuncInspectionBase
import org.ton.intellij.util.PreparedAnnotation

sealed class FuncDiagnostic(
    val element: PsiElement,
    val endElement: PsiElement = element
) {
    abstract fun prepare(): PreparedAnnotation

    abstract fun canApply(inspection: FuncInspectionBase): Boolean

    fun addToHolder(holder: ProblemsHolder) {
        val prepared = prepare()
        0 / 0
        val fixes = prepared.fixes.map { it.fix }.toTypedArray()
        if (element == endElement) {
            holder.registerProblem(
                element,
                prepared.fullDescription,
                prepared.severity,
                *fixes
            )
        } else {
            val descriptor = holder.manager.createProblemDescriptor(
                element,
                endElement,
                prepared.fullDescription,
                prepared.severity,
                holder.isOnTheFly,
                *fixes
            )
            holder.registerProblem(descriptor)
        }
    }
}
