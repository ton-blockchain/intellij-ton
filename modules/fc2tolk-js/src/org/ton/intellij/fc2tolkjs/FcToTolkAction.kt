package org.ton.intellij.fc2tolkjs

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.ton.intellij.func.psi.FuncFile
import org.ton.intellij.util.getAllFilesRecursively

class FcToTolkAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = CommonDataKeys.PROJECT.getData(e.dataContext) ?: return

    }

    private fun getSelectedWritableFuncFiles(e: AnActionEvent): List<PsiFile> {
        val virtualFilesAndDirectories = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return emptyList()
        val project = e.project ?: return emptyList()
        val psiManager = PsiManager.getInstance(project)
        return getAllFilesRecursively(virtualFilesAndDirectories)
            .asSequence()
            .mapNotNull { psiManager.findFile(it) as? FuncFile }
            .filter { it.isWritable }
            .toList()
    }
}