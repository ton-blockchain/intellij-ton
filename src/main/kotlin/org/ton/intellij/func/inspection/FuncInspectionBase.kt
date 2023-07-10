package org.ton.intellij.func.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
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

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return super.buildVisitor(holder, isOnTheFly)
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

    open protected fun buildFuncVisitor(
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
}
