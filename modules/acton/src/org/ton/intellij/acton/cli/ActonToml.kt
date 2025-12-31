package org.ton.intellij.acton.cli

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.toml.lang.psi.TomlFile
import org.toml.lang.psi.TomlTable

import java.nio.file.Path

class ActonToml(val virtualFile: VirtualFile, val project: Project) {
    private val psiFile: TomlFile? get() = PsiManager.getInstance(project).findFile(virtualFile) as? TomlFile

    val workingDir: Path get() = virtualFile.parent.toNioPath()

    fun getContractIds(): List<String> {
        val file = psiFile ?: return emptyList()

        return PsiTreeUtil.getChildrenOfType(file, TomlTable::class.java)
            ?.mapNotNull { table ->
                val segments = table.header.key?.segments ?: return@mapNotNull null
                if (segments.size == 2 && segments[0].name == "contracts") {
                    segments[1].name
                } else {
                    null
                }
            } ?: emptyList()
    }

    companion object {
        fun find(project: Project): ActonToml? {
            val files = FilenameIndex.getVirtualFilesByName("Acton.toml", GlobalSearchScope.projectScope(project))
            return files.firstOrNull()?.let { ActonToml(it, project) }
        }
    }
}
