package org.ton.intellij.acton.cli

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
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

    val workingDir: Path get() = Path.of(virtualFile.parent.path)

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
                ?.find { it.header.key?.segments?.firstOrNull()?.name == "import-mappings" }

            val result = mappingsTable?.entries?.associate { entry ->
                val key = entry.key.segments.firstOrNull()?.name?.removePrefix("@") ?: entry.key.text
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

    fun getContractIds(): List<String> = getContracts().map { it.id }

    fun getContractSources(): List<String> = getContracts().mapNotNull { it.sourcePath }

    fun getContractElements(): List<TomlKeySegment> = getContracts().map { it.element }

    data class ContractInfo(val id: String, val sourcePath: String?, val element: TomlKeySegment)

    fun getContracts(): List<ContractInfo> {
        return CachedValuesManager.getCachedValue(psiFile ?: return emptyList()) {
            val file = psiFile ?: return@getCachedValue create(emptyList(), PsiModificationTracker.MODIFICATION_COUNT)
            val result = PsiTreeUtil.getChildrenOfType(file, TomlTable::class.java)
                ?.mapNotNull { table ->
                    val segments = table.header.key?.segments ?: return@mapNotNull null
                    if (segments.size != 2 || segments[0].name != "contracts") return@mapNotNull null

                    val id = segments[1].name ?: return@mapNotNull null
                    val sourcePath = table.entries
                        .find { it.key.text == "src" }
                        ?.value
                        ?.text
                        ?.removeSurrounding("\"")
                        ?.removeSurrounding("'")

                    ContractInfo(id, sourcePath, segments[1])
                }
                ?: emptyList()

            create(result, file)
        }
    }

    fun findContractIdBySource(file: VirtualFile): String? {
        val filePath = normalizePath(file.path)
        return getContracts()
            .firstOrNull { contract ->
                val sourcePath = contract.sourcePath ?: return@firstOrNull false
                resolveConfiguredPath(sourcePath) == filePath
            }
            ?.id
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

    fun getCustomNetworkNames(): List<String> = getCustomNetworkElements().mapNotNull { it.name }.distinct()

    fun getCustomNetworkElements(): List<TomlKeySegment> {
        val file = psiFile ?: return emptyList()

        return PsiTreeUtil.getChildrenOfType(file, TomlTable::class.java)
            ?.mapNotNull { table ->
                val segments = table.header.key?.segments ?: return@mapNotNull null
                if (segments.size == 2 && segments[0].name == "networks") {
                    segments[1]
                } else {
                    null
                }
            }
            ?: emptyList()
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

    private fun resolveConfiguredPath(path: String): String {
        val normalized = Path.of(path)
        val resolved = if (normalized.isAbsolute) normalized else workingDir.resolve(normalized)
        return normalizePath(resolved.normalize().toString())
    }

    private fun normalizePath(path: String): String = path.replace('\\', '/')

    companion object {
        /**
         * Finds the Acton configuration at the IDE project root.
         *
         * Use this overload only for project-level flows that do not have a source file or directory context.
         * File-based features should use [find] with a [VirtualFile] so nested Acton projects resolve their nearest
         * configuration instead of the project-root one.
         */
        fun find(project: Project): ActonToml? = CachedValuesManager.getManager(project).getCachedValue(project) {
            val projectDir = project.guessProjectDir()
            val virtualFile = projectDir?.findChild(ACTON_TOML)?.takeUnless { it.isDirectory }

            val actonToml = virtualFile?.let { ActonToml(it, project) }
            create(
                actonToml,
                com.intellij.openapi.vfs.VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS,
                PsiModificationTracker.MODIFICATION_COUNT,
            )
        }

        /**
         * Finds the nearest Acton configuration for [from].
         *
         * The search starts from [from] itself when it is a directory, or from its parent when it is a file, and walks
         * upward until the IDE project root. For files outside the project root but inside a content root, the content
         * root is used as the boundary. The search intentionally does not fall back to an arbitrary `Acton.toml` from
         * the whole project scope.
         */
        fun find(project: Project, from: VirtualFile): ActonToml? {
            val startDir = if (from.isDirectory) from else from.parent ?: return null
            val stopDir = findSearchRoot(project, startDir) ?: return null

            var dir: VirtualFile? = startDir
            while (dir != null) {
                dir.findChild(ACTON_TOML)?.takeUnless { it.isDirectory }?.let { return ActonToml(it, project) }
                if (dir == stopDir) break
                dir = dir.parent
            }
            return null
        }

        private fun findSearchRoot(project: Project, startDir: VirtualFile): VirtualFile? {
            val projectDir = project.guessProjectDir()
            if (projectDir != null && (projectDir == startDir || VfsUtilCore.isAncestor(projectDir, startDir, false))) {
                return projectDir
            }

            val contentRoot = ProjectFileIndex.getInstance(project).getContentRootForFile(startDir)
            if (contentRoot != null) {
                return contentRoot
            }

            return null
        }

        private const val ACTON_TOML = "Acton.toml"
    }
}
