package org.ton.intellij.tolk.ide.configurable

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
import com.intellij.util.messages.Topic
import org.ton.intellij.tolk.psi.TolkFile

val Project.tolkSettings: TolkProjectSettingsService get() = service()

@State(
    name = "TolkProjectSettings",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
@Service(Service.Level.PROJECT)
class TolkProjectSettingsService(
    private val project: Project,
) : SimplePersistentStateComponent<TolkProjectSettingsService.TolkProjectSettings>(TolkProjectSettings()) {

    var stdlibPath: String?
        get() {
            val path = state.stdlibPath
            if (path != null) return path
            return findStdlib()?.path
        }
        set(value) {
            state.stdlibPath = value
            defaultImport = null
            notifySettingsChanged()
            reloadProject()
        }

    private fun findStdlib(): VirtualFile? {
        val projectDir = project.guessProjectDir() ?: return null
        val candidates = listOf(
            ".acton/tolk-stdlib", // Acton
            "node_modules/@ton/tolk-js/tolk-stdlib", // Blueprint
            "node_modules/@ton/tolk-js/dist/tolk-stdlib", // Blueprint
            "crypto/smartcont/tolk-stdlib", // TON monorepo
        )
        for (candidate in candidates) {
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

    private var defaultImport: TolkFile? = null

    val hasStdlib get() = getDefaultImport() != null

    fun getDefaultImport(): TolkFile? {
        val currentDefaultImport = defaultImport
        if (currentDefaultImport == null) {
            val result = stdlibDir?.findFile("common.tolk")?.findPsiFile(project) as? TolkFile
            defaultImport = result
            return result
        }
        return currentDefaultImport
    }

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
                Topic.BroadcastDirection.TO_PARENT
            )
        }
    }
}
