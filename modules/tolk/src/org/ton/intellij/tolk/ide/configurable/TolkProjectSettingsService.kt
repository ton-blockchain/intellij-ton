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
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.findFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.messages.Topic
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.tolkPsiManager

val Project.tolkSettings: TolkProjectSettingsService get() = service()

@State(
    name = "TolkProjectSettings",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
)
@Service(Service.Level.PROJECT)
class TolkProjectSettingsService(private val project: Project) :
    SimplePersistentStateComponent<TolkProjectSettingsService.TolkProjectSettings>(TolkProjectSettings()),
    Disposable {

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

    private fun findStdlib(): VirtualFile? {
        val projectDir = project.guessProjectDir() ?: return null
        for (candidate in STDLIB_CANDIDATES) {
            val file = projectDir.findFileByRelativePath(candidate)
            if (file != null && file.isDirectory) {
                return file
            }
        }
        return null
    }

    val stdlibDir: VirtualFile?
        get() {
            return stdlibPath?.let {
                val vfm = VirtualFileManager.getInstance()
                vfm.findFileByUrl(it) ?: vfm.findFileByNioPath(it.toNioPathOrNull() ?: return null)
            }
        }

    private val defaultImport = CachedValuesManager.getManager(project).createCachedValue({
        val result = stdlibDir?.findFile("common.tolk")?.findPsiFile(project) as? TolkFile
        CachedValueProvider.Result.create(result, project.tolkPsiManager.tolkStructureModificationCount)
    }, false)

    val hasStdlib get() = getDefaultImport() != null

    fun getDefaultImport(): TolkFile? {
        val cachedDefaultImport = defaultImport.value
        if (
            cachedDefaultImport == null ||
            (cachedDefaultImport.isValid && cachedDefaultImport.virtualFile?.isValid == true)
        ) {
            return cachedDefaultImport
        }
        return stdlibDir?.findFile("common.tolk")?.findPsiFile(project) as? TolkFile
    }

    private fun classifyStdlibChange(event: VFileEvent): StdlibChangeKind? {
        val eventPath = normalizePath(event.path)
        return expectedStdlibPaths().firstNotNullOfOrNull { stdlibPath ->
            when {
                eventPath == stdlibPath -> StdlibChangeKind.ROOTS
                stdlibPath.startsWith("$eventPath/") -> StdlibChangeKind.ROOTS
                eventPath.startsWith("$stdlibPath/") -> StdlibChangeKind.CONTENTS
                else -> null
            }
        }
    }

    private fun expectedStdlibPaths(): List<String> {
        val configuredPath = state.stdlibPath?.let(::normalizePath)
        if (configuredPath != null) {
            return listOf(configuredPath)
        }

        val projectDir = project.guessProjectDir()?.path?.let(::normalizePath) ?: return emptyList()
        return STDLIB_CANDIDATES.map { candidate -> normalizePath("$projectDir/$candidate") }
    }

    private fun normalizePath(path: String): String = path.substringAfter("://", path).replace('\\', '/').trimEnd('/')

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
