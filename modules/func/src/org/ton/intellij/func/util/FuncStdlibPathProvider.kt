package org.ton.intellij.func.util

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import org.ton.intellij.func.FuncFileType
import org.ton.intellij.func.psi.FuncFile
import org.ton.intellij.func.psi.impl.path

object FuncStdlibPathProvider {
    fun getStdlibPath(project: Project): String {
        val funcFiles = FileTypeIndex.getFiles(FuncFileType, GlobalSearchScope.projectScope(project))
        val psiManager = PsiManager.getInstance(project)

        val stdlibPaths = mutableMapOf<String, Int>()

        for (virtualFile in funcFiles) {
            val funcFile = psiManager.findFile(virtualFile) as? FuncFile ?: continue

            for (includeDefinition in funcFile.includeDefinitions) {
                val path = includeDefinition.path
                if (path.endsWith("stdlib.fc")) {
                    stdlibPaths[path] = stdlibPaths.getOrDefault(path, 0) + 1
                }
            }
        }

        if (stdlibPaths.isNotEmpty()) {
            return stdlibPaths.maxByOrNull { it.value }?.key ?: "stdlib.fc"
        }

        return "stdlib.fc"
    }
}
