package org.ton.intellij.acton.cli

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider.Result.create
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import org.toml.lang.psi.TomlFile
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlTable
import java.nio.file.Path

class ActonToml(val virtualFile: VirtualFile, val project: Project) {
    private val psiFile: TomlFile? get() = PsiManager.getInstance(project).findFile(virtualFile) as? TomlFile

    val workingDir: Path get() = virtualFile.parent.toNioPath()

    fun getScripts(): Map<String, String> {
        return CachedValuesManager.getCachedValue(psiFile ?: return emptyMap()) {
            val scriptsTable = PsiTreeUtil.getChildrenOfType(psiFile, TomlTable::class.java)
                ?.find { it.header.key?.segments?.firstOrNull()?.name == "scripts" }
            
            val result = scriptsTable?.entries?.associate { entry ->
                val key = entry.key.segments.firstOrNull()?.name ?: entry.key.text
                val value = entry.value?.text?.removeSurrounding("\"")?.removeSurrounding("'") ?: ""
                key to value
            } ?: emptyMap()

            create(result, psiFile)
        }
    }

    fun getMappings(): Map<String, String> {
        return CachedValuesManager.getCachedValue(psiFile ?: return emptyMap()) {
            val mappingsTable = PsiTreeUtil.getChildrenOfType(psiFile, TomlTable::class.java)
                ?.find { it.header.key?.segments?.firstOrNull()?.name == "mappings" }
            
            val result = mappingsTable?.entries?.associate { entry ->
                val key = entry.key.segments.firstOrNull()?.name ?: entry.key.text
                val value = entry.value?.text?.removeSurrounding("\"")?.removeSurrounding("'") ?: ""
                key to value
            } ?: emptyMap()

            create(result, psiFile)
        }
    }

    fun getNormalizedMappings(): Map<String, String> {
        return CachedValuesManager.getCachedValue(psiFile ?: return emptyMap()) {
            val mappings = getMappings()
            val result = mappings.mapValues { (_, value) ->
                workingDir.resolve(value).normalize().toString().replace('\\', '/')
            }
            create(result, psiFile)
        }
    }

    fun getContractIds(): List<String> {
        return getContractElements().map { it.name ?: "" }
    }

    fun getContractSources(): List<String> {
        val file = psiFile ?: return emptyList()

        return PsiTreeUtil.getChildrenOfType(file, TomlTable::class.java)
            ?.mapNotNull { table ->
                val segments = table.header.key?.segments ?: return@mapNotNull null
                if (segments.size == 2 && segments[0].name == "contracts") {
                    table.entries.find { it.key.text == "src" }?.value?.text?.removeSurrounding("\"")?.removeSurrounding("'")
                } else {
                    null
                }
            } ?: emptyList()
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
            return CachedValuesManager.getManager(project).getCachedValue(project) {
                val projectDir = project.guessProjectDir()
                val actonTomlFile = projectDir?.findChild("Acton.toml")
                val virtualFile = actonTomlFile ?: FilenameIndex.getVirtualFilesByName("Acton.toml", GlobalSearchScope.projectScope(project)).firstOrNull()
                
                val actonToml = virtualFile?.let { ActonToml(it, project) }
                create(actonToml, com.intellij.openapi.vfs.VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS, PsiModificationTracker.MODIFICATION_COUNT)
            }
        }
    }
}
