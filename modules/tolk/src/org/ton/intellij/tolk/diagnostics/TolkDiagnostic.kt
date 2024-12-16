package org.ton.intellij.tolk.diagnostics

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.inspection.TolkInspectionBase
import org.ton.intellij.util.PreparedAnnotation

abstract class TolkDiagnostic(
    val element: PsiElement,
    val endElement: PsiElement = element
) {
    abstract fun prepare(): PreparedAnnotation

    abstract fun canApply(inspection: TolkInspectionBase): Boolean

    fun addToHolder(holder: ProblemsHolder) {
        val prepared = prepare()
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
