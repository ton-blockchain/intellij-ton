package org.ton.intellij.func.inspection

import com.intellij.codeInspection.*
import com.intellij.codeInspection.util.InspectionMessage
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import org.ton.intellij.func.psi.FuncFile
import org.ton.intellij.func.psi.FuncPsiUtil
import org.ton.intellij.func.psi.FuncVisitor

abstract class FuncInspectionBase : LocalInspectionTool() {
    companion object {
        private val DUMMY_VISITOR = object : FuncVisitor() {
        }
    }

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ): PsiElementVisitor {
        val file = session.file as? FuncFile
        return if (file != null && FuncPsiUtil.allowed(file, null)) {
            buildFuncVisitor(holder, session)
        } else DUMMY_VISITOR
    }

    protected open fun buildFuncVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): FuncVisitor = object : FuncVisitor() {
        override fun visitFile(file: PsiFile) {
            if (file is FuncFile) {
                checkFile(file, holder)
            }
        }
    }

    protected fun checkFile(file: FuncFile, problemsHolder: ProblemsHolder) {
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
