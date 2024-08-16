package org.ton.intellij.tolk.psi

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile

object TolkPsiUtil {
    fun allowed(declarationFile: PsiFile, referenceFile: PsiFile?, contextModule: Module? = null): Boolean {
        if (declarationFile !is TolkFile) return false
        val referenceVirtualFile = referenceFile?.originalFile?.virtualFile
        return allowed(declarationFile.virtualFile, referenceVirtualFile)
        // TODO: matchedForModuleBuildTarget
    }

    fun allowed(declarationFile: VirtualFile?, referenceFile: VirtualFile?): Boolean {
        if (declarationFile == null) return true
        return referenceFile == null || referenceFile.parent == declarationFile.parent
    }
}
