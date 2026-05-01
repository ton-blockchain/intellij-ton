package org.ton.intellij.tolk.ide.configurable

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.*
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.RootsChangeRescanningInfo
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.findFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.messages.Topic
import org.ton.intellij.acton.cli.ActonToml
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.tolkPsiManager
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

val Project.tolkSettings: TolkProjectSettingsService get() = service()

@State(
    name = "TolkProjectSettings",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
)
@Service(Service.Level.PROJECT)
class TolkProjectSettingsService(private val project: Project) :
    SimplePersistentStateComponent<TolkProjectSettingsService.TolkProjectSettings>(TolkProjectSettings()),
    Disposable {
    private val cachedActonRootsByStartDirUrl = ConcurrentHashMap<String, CachedValue<VirtualFile?>>()
    private val cachedStdlibDirsByRootUrl = ConcurrentHashMap<String, CachedValue<VirtualFile?>>()
    private val cachedDefaultImportsByStdlibUrl = ConcurrentHashMap<String, CachedValue<TolkFile?>>()

    init {
        project.messageBus.connect(this).subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    val changeKind = events.asSequence()
                        .filterNot { it is VFileContentChangeEvent }
                        .mapNotNull(::classifyStdlibChange)
                        .maxOrNull()

                    when (changeKind) {
                        StdlibChangeKind.ROOTS -> {
                            notifySettingsChanged()
                            reloadProject()
                        }

                        StdlibChangeKind.CONTENTS -> {
                            project.tolkPsiManager.incTolkSignatureModificationCount()
                        }

                        null -> Unit
                    }
                }
            },
        )
    }

    override fun dispose() = Unit

    var stdlibPath: String?
        get() {
            val path = state.stdlibPath
            if (path != null) return path
            return findStdlib()?.path
        }
        set(value) {
            state.stdlibPath = value
            project.tolkPsiManager.incTolkSignatureModificationCount()
            notifySettingsChanged()
            reloadProject()
        }

    private fun findStdlib(): VirtualFile? = findStdlib(null)

    private fun findStdlib(contextFile: VirtualFile?): VirtualFile? {
        for (root in stdlibSearchRoots(contextFile)) {
            cachedStdlibDir(root)?.let { return it }
        }
        return null
    }

    val stdlibDir: VirtualFile?
        get() {
            return configuredStdlibDir() ?: findStdlib()
        }

    fun stdlibDirFor(contextFile: VirtualFile?): VirtualFile? {
        val configuredStdlibDir = configuredStdlibDir()
        if (configuredStdlibDir != null) return configuredStdlibDir
        return findStdlib(contextFile)
    }

    fun stdlibDirs(): List<VirtualFile> {
        val configuredStdlibDir = configuredStdlibDir()
        if (configuredStdlibDir != null) return listOf(configuredStdlibDir)

        return stdlibSearchRoots(null)
            .flatMap { root ->
                STDLIB_CANDIDATES.mapNotNull { candidate ->
                    root.findFileByRelativePath(candidate)?.takeIf { it.isDirectory }
                }
            }
            .distinctBy { it.path }
    }

    private fun configuredStdlibDir(): VirtualFile? {
        val configuredPath = state.stdlibPath ?: return null
        val vfm = VirtualFileManager.getInstance()
        return vfm.findFileByUrl(configuredPath)
            ?: vfm.findFileByNioPath(configuredPath.toNioPathOrNull() ?: return null)
    }

    val hasStdlib get() = getDefaultImport() != null

    fun hasStdlibFor(contextFile: VirtualFile?): Boolean =
        if (contextFile != null) getDefaultImport(contextFile) != null else hasStdlib

    fun getDefaultImport(): TolkFile? {
        val stdlibDir = stdlibDir ?: return null
        return cachedDefaultImport(stdlibDir)
    }

    fun getDefaultImport(contextFile: VirtualFile): TolkFile? = stdlibDirFor(contextFile)?.let(::cachedDefaultImport)

    fun refreshDetectedStdlib(contextFile: VirtualFile? = null) {
        val hadStdlib = hasStdlibFor(contextFile)

        refreshExpectedStdlibPaths(contextFile)
        project.tolkPsiManager.incTolkSignatureModificationCount()
        notifySettingsChanged()

        if (hadStdlib != hasStdlibFor(contextFile)) {
            reloadProject()
        }
    }

    private fun refreshExpectedStdlibPaths(contextFile: VirtualFile?) {
        val files = expectedStdlibPaths(contextFile).mapNotNull { path ->
            runCatching { Path.of(path).toFile() }.getOrNull()
        }

        if (files.isNotEmpty()) {
            LocalFileSystem.getInstance().refreshIoFiles(files)
        }
    }

    private fun classifyStdlibChange(event: VFileEvent): StdlibChangeKind? {
        val eventPath = normalizePath(event.path)
        if (!isInsideProjectPath(eventPath)) return null
        return classifyExpectedStdlibChange(eventPath) ?: classifyNestedStdlibChange(eventPath)
    }

    private fun classifyExpectedStdlibChange(eventPath: String): StdlibChangeKind? =
        expectedStdlibPaths(null).firstNotNullOfOrNull { stdlibPath ->
            when {
                eventPath == stdlibPath -> StdlibChangeKind.ROOTS
                stdlibPath.startsWith("$eventPath/") -> StdlibChangeKind.ROOTS
                eventPath.startsWith("$stdlibPath/") -> StdlibChangeKind.CONTENTS
                else -> null
            }
        }

    private fun classifyNestedStdlibChange(eventPath: String): StdlibChangeKind? =
        STDLIB_CANDIDATES.firstNotNullOfOrNull { candidate ->
            when {
                eventPath.endsWith("/$candidate") -> StdlibChangeKind.ROOTS
                "/$candidate/" in eventPath -> StdlibChangeKind.CONTENTS
                candidate.startsWith(".acton/") && eventPath.endsWith("/.acton") -> StdlibChangeKind.ROOTS
                else -> null
            }
        }

    private fun expectedStdlibPaths(contextFile: VirtualFile?): List<String> {
        val configuredPath = state.stdlibPath?.let(::normalizePath)
        if (configuredPath != null) {
            return listOf(configuredPath)
        }

        return stdlibSearchRoots(contextFile).flatMap { root ->
            STDLIB_CANDIDATES.map { candidate -> normalizePath("${root.path}/$candidate") }
        }
    }

    private fun stdlibSearchRoots(contextFile: VirtualFile?): List<VirtualFile> {
        contextFile?.let { file ->
            cachedActonRoot(file)?.let { return listOf(it) }
        }
        return listOfNotNull(project.guessProjectDir())
    }

    private fun cachedActonRoot(contextFile: VirtualFile): VirtualFile? {
        val startDir = if (contextFile.isDirectory) contextFile else contextFile.parent ?: return null
        return cachedActonRootsByStartDirUrl.computeIfAbsent(startDir.url) { startDirUrl ->
            CachedValuesManager.getManager(project).createCachedValue({
                val currentStartDir = VirtualFileManager.getInstance().findFileByUrl(startDirUrl)
                val result = currentStartDir?.let { ActonToml.find(project, it)?.virtualFile?.parent }
                CachedValueProvider.Result.create(result, VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS)
            }, false)
        }.value
    }

    private fun cachedStdlibDir(root: VirtualFile): VirtualFile? =
        cachedStdlibDirsByRootUrl.computeIfAbsent(root.url) { rootUrl ->
            CachedValuesManager.getManager(project).createCachedValue({
                val currentRoot = VirtualFileManager.getInstance().findFileByUrl(rootUrl)
                val result = STDLIB_CANDIDATES.firstNotNullOfOrNull { candidate ->
                    currentRoot?.findFileByRelativePath(candidate)?.takeIf { it.isDirectory }
                }
                CachedValueProvider.Result.create(result, VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS)
            }, false)
        }.value

    private fun cachedDefaultImport(stdlibDir: VirtualFile): TolkFile? =
        cachedDefaultImportsByStdlibUrl.computeIfAbsent(stdlibDir.url) { stdlibUrl ->
            CachedValuesManager.getManager(project).createCachedValue({
                val currentStdlibDir = VirtualFileManager.getInstance().findFileByUrl(stdlibUrl)
                val result = currentStdlibDir?.findFile("common.tolk")?.findPsiFile(project) as? TolkFile
                CachedValueProvider.Result.create(result, VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS)
            }, false)
        }.value

    private fun normalizePath(path: String): String = path.substringAfter("://", path).replace('\\', '/').trimEnd('/')

    private fun isInsideProjectPath(path: String): Boolean {
        val projectDir = project.guessProjectDir() ?: return true
        return isSameOrUnder(path, normalizePath(projectDir.path))
    }

    private fun isSameOrUnder(path: String, rootPath: String): Boolean =
        path == rootPath || path.startsWith("$rootPath/")

    private fun reloadProject() {
        invokeLater(modalityState = ModalityState.nonModal()) {
            WriteAction.run<RuntimeException> {
                ProjectRootManagerEx.getInstanceEx(project).makeRootsChange({}, RootsChangeRescanningInfo.TOTAL_RESCAN)
            }
        }
    }

    fun notifySettingsChanged() {
        project.messageBus.syncPublisher(TolkSettingsListener.TOPIC).tolkSettingsChanged()
    }

    fun configureToolchain() {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, TolkProjectConfigurable::class.java)
    }

    class TolkProjectSettings : BaseState() {
        var stdlibPath by string()

        fun copy() = TolkProjectSettings().also { it.copyFrom(this) }
    }

    interface TolkSettingsListener {
        fun tolkSettingsChanged()

        companion object {
            val TOPIC = Topic.create(
                "Tolk settings changes",
                TolkSettingsListener::class.java,
                Topic.BroadcastDirection.TO_PARENT,
            )
        }
    }

    companion object {
        private val STDLIB_CANDIDATES = listOf(
            ".acton/tolk-stdlib", // Acton
            "node_modules/@ton/tolk-js/tolk-stdlib", // npm package layout
            "node_modules/@ton/tolk-js/dist/tolk-stdlib", // npm package layout
            "crypto/smartcont/tolk-stdlib", // TON monorepo
        )
    }

    private enum class StdlibChangeKind {
        CONTENTS,
        ROOTS,
    }
}
