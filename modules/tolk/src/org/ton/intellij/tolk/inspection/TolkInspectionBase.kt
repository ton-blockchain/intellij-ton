package org.ton.intellij.tolk.inspection

import com.intellij.codeInspection.*
import com.intellij.codeInspection.util.InspectionMessage
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.TolkPsiUtil
import org.ton.intellij.tolk.psi.TolkVisitor

abstract class TolkInspectionBase : LocalInspectionTool() {
    companion object {
        private val DUMMY_VISITOR = object : TolkVisitor() {
        }
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return super.buildVisitor(holder, isOnTheFly)
    }

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ): PsiElementVisitor {
        val file = session.file as? TolkFile
        return if (file != null && TolkPsiUtil.allowed(file, null)) {
            buildTolkVisitor(holder, session)
        } else DUMMY_VISITOR
    }

    open protected fun buildTolkVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): TolkVisitor = object : TolkVisitor() {
        override fun visitFile(file: PsiFile) {
            if (file is TolkFile) {
                checkFile(file, holder)
            }
        }
    }

    protected fun checkFile(file: TolkFile, problemsHolder: ProblemsHolder) {
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
