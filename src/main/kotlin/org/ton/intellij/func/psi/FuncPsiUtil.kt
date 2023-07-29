package org.ton.intellij.func.psi

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile

object FuncPsiUtil {
    fun allowed(declarationFile: PsiFile, referenceFile: PsiFile?, contextModule: Module? = null): Boolean {
        if (declarationFile !is FuncFile) return false
        val referenceVirtualFile = referenceFile?.originalFile?.virtualFile
        return allowed(declarationFile.virtualFile, referenceVirtualFile)
        // TODO: matchedForModuleBuildTarget
    }

    fun allowed(declarationFile: VirtualFile?, referenceFile: VirtualFile?): Boolean {
        if (declarationFile == null) return true
        return referenceFile == null || referenceFile.parent == declarationFile.parent
    }
}

val FuncCallExpression.isQualified: Boolean
    get() {
        if (tilde != null) return true
        val parent = parent
        if (parent !is FuncQualifiedExpression) return false
        val expressionList = parent.expressionList
        return expressionList.size == 2 && expressionList.last() == this
    }
