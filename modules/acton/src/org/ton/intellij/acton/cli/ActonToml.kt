package org.ton.intellij.acton.cli

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.toml.lang.psi.TomlFile
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlTable
import java.nio.file.Path

class ActonToml(val virtualFile: VirtualFile, val project: Project) {
    private val psiFile: TomlFile? get() = PsiManager.getInstance(project).findFile(virtualFile) as? TomlFile

    val workingDir: Path get() = virtualFile.parent.toNioPath()

    fun getScripts(): Map<String, String> {
        val file = psiFile ?: return emptyMap()
        val scriptsTable = PsiTreeUtil.getChildrenOfType(file, TomlTable::class.java)
            ?.find { it.header.key?.segments?.firstOrNull()?.name == "scripts" }
            ?: return emptyMap()

        return scriptsTable.entries.associate { entry ->
            entry.key.text to (entry.value?.text?.removeSurrounding("\"") ?: "")
        }
    }

    fun getContractIds(): List<String> {
        return getContractElements().map { it.name ?: "" }
    }

    fun getContractElements(): List<TomlKeySegment> {
        val file = psiFile ?: return emptyList()

        return PsiTreeUtil.getChildrenOfType(file, TomlTable::class.java)
            ?.mapNotNull { table ->
                val segments = table.header.key?.segments ?: return@mapNotNull null
                if (segments.size == 2 && segments[0].name == "contracts") {
                    segments[1]
                } else {
                    null
                }
            } ?: emptyList()
    }

    data class WalletInfo(val name: String, val isLocal: Boolean, val element: TomlKeySegment? = null)

    fun getWallets(): List<WalletInfo> {
        val result = mutableListOf<WalletInfo>()
        val projectDir = virtualFile.parent ?: return emptyList()

        projectDir.findChild("wallets.toml")?.let {
            val elements = getWalletElementsFromPsi(PsiManager.getInstance(project).findFile(it) as? TomlFile)
            result.addAll(elements.map { el -> WalletInfo(el.name ?: "", true, el) })
        }

        // add global wallets later to filter it by `distinctBy`, since local > global
        projectDir.findChild("global.wallets.toml")?.let {
            val elements = getWalletElementsFromPsi(PsiManager.getInstance(project).findFile(it) as? TomlFile)
            result.addAll(elements.map { el -> WalletInfo(el.name ?: "", false, el) })
        }

        return result.distinctBy { it.name }
    }

    private fun getWalletElementsFromPsi(file: TomlFile?): List<TomlKeySegment> {
        if (file == null) return emptyList()
        return PsiTreeUtil.getChildrenOfType(file, TomlTable::class.java)
            ?.mapNotNull { table ->
                val segments = table.header.key?.segments ?: return@mapNotNull null
                if (segments.size == 2 && segments[0].name == "wallets") {
                    segments[1]
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
