package org.ton.intellij.tact.diagnostics

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import org.ton.intellij.tact.inspections.TactLocalInspectionTool
import org.ton.intellij.tact.inspections.TactTypeCheckInspection
import org.ton.intellij.tact.psi.TactNamedElement
import org.ton.intellij.tact.type.TactTy
import org.ton.intellij.util.PreparedAnnotation

sealed class TactDiagnostic(
    val element: PsiElement,
    val endElement: PsiElement = element
) {
    abstract fun prepare(): PreparedAnnotation

    abstract fun canApply(localInspectionTool: TactLocalInspectionTool): Boolean

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

    class TypeError(
        element: PsiElement,
        val expectedTy: TactTy,
        val actualTy: TactTy
    ) : TactDiagnostic(element) {
        override fun prepare(): PreparedAnnotation = PreparedAnnotation(
            ProblemHighlightType.GENERIC_ERROR,
            "Type mismatch",
            "expected `$expectedTy`, found `$actualTy`",
            fixes = buildList {

            }
        )

        override fun canApply(localInspectionTool: TactLocalInspectionTool): Boolean {
            return localInspectionTool is TactTypeCheckInspection
        }
    }

    class VariableAlreadyExists(
        element: PsiElement,
        val definition: TactNamedElement
    ) : TactDiagnostic(element) {
        override fun prepare(): PreparedAnnotation = PreparedAnnotation(
            ProblemHighlightType.GENERIC_ERROR,
            "Variable already exists",
            "`${definition.name}` is already defined in this scope",
            fixes = buildList {

            }
        )

        override fun canApply(localInspectionTool: TactLocalInspectionTool): Boolean {
            return false
        }
    }
}
