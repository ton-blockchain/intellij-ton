package org.ton.intellij.tlb.inspection

import com.intellij.codeInspection.*
import com.intellij.codeInspection.util.InspectionMessage
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import org.ton.intellij.tlb.psi.TlbFile
import org.ton.intellij.tlb.psi.TlbVisitor

abstract class TlbInspectionBase : LocalInspectionTool() {
    companion object {
        private val DUMMY_VISITOR = object : TlbVisitor() {
        }
    }

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ): PsiElementVisitor {
        val file = session.file as? TlbFile
        return if (file != null) {
            buildTlbVisitor(holder, session)
        } else DUMMY_VISITOR
    }

    protected open fun buildTlbVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): TlbVisitor = object : TlbVisitor() {
        override fun visitFile(file: PsiFile) {
            if (file is TlbFile) {
                checkFile(file, holder)
            }
        }
    }

    protected fun checkFile(file: TlbFile, problemsHolder: ProblemsHolder) {
    }

    fun ProblemsHolder.registerProblemWithoutOfflineInformation(
        element: PsiElement,
        @InspectionMessage description: String,
        isOnTheFly: Boolean,
        highlightType: ProblemHighlightType,
        vararg fixes: LocalQuickFix,
    ) {
        registerProblemWithoutOfflineInformation(element, description, isOnTheFly, highlightType, null, *fixes)
    }

    fun ProblemsHolder.registerProblemWithoutOfflineInformation(
        element: PsiElement,
        @InspectionMessage description: String,
        isOnTheFly: Boolean,
        highlightType: ProblemHighlightType,
        range: TextRange?,
        vararg fixes: LocalQuickFix,
    ) {
        if (!isOnTheFly && highlightType == ProblemHighlightType.INFORMATION) return
        val problemDescriptor =
            manager.createProblemDescriptor(element, range, description, highlightType, isOnTheFly, *fixes)
        registerProblem(problemDescriptor)
    }
}
