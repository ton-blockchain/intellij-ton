package org.ton.intellij.func.psi

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile

object FuncPsiUtil {
    fun allowed(declarationFile: PsiFile, referenceFile: PsiFile?, contextModule: Module? = null): Boolean {
        if (declarationFile !is FuncFile) return false
        val referenceVirtualFile = referenceFile?.originalFile?.virtualFile
        if (!allowed(declarationFile.virtualFile, referenceVirtualFile)) return false
        // TODO: matchedForModuleBuildTarget
        return true
    }

    fun allowed(declarationFile: VirtualFile?, referenceFile: VirtualFile?): Boolean {
        if (declarationFile == null) return true
        return referenceFile == null || referenceFile.parent == declarationFile.parent
    }
}
