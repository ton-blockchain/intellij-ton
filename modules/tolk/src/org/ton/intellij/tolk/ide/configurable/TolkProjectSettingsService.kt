package org.ton.intellij.tolk.ide.configurable

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.*
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.RootsChangeRescanningInfo
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.findFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.util.messages.Topic
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.toolchain.TolkToolchain

val Project.tolkSettings: TolkProjectSettingsService get() = service()

@State(
    name = "TolkProjectSettings",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
@Service(Service.Level.PROJECT)
class TolkProjectSettingsService(
    private val project: Project
) : SimplePersistentStateComponent<TolkProjectSettingsService.TolkProjectSettings>(TolkProjectSettings()) {
    private var _toolchain: TolkToolchain? = null

    var toolchain: TolkToolchain
        get() {
            var currentToolchain = _toolchain
            val currentLocation = state.toolchainLocation
            if (currentToolchain == null && !currentLocation.isNullOrEmpty()) {
                currentToolchain = TolkToolchain.fromPath(currentLocation)
                _toolchain = currentToolchain
            }
            return currentToolchain ?: TolkToolchain.NULL
        }
        set(value) {
            _toolchain = value
            state.toolchainLocation = value.homePath.ifEmpty { null }
            defaultImport = null
            notifySettingsChanged()
            reloadProject()
        }
    var explicitPathToStdlib: String?
        get() = state.explicitPathToStdlib
        set(value) {
            state.explicitPathToStdlib = value
            defaultImport = null
            notifySettingsChanged()
            reloadProject()
        }
    var testToolPath: String?
        get() = state.testToolPath
        set(value) {
            state.testToolPath = value
            notifySettingsChanged()
        }
    val stdlibDir: VirtualFile?
        get() {
            return explicitPathToStdlib?.let {
                val vfm = VirtualFileManager.getInstance()
                vfm.findFileByUrl(it) ?: vfm.findFileByNioPath(it.toNioPathOrNull() ?: return null)
            } ?: toolchain.stdlibDir
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
//        DaemonCodeAnalyzer.getInstance(project).restart()
//        runWriteAction {
//            ProjectRootManagerEx.getInstanceEx(project)
//                .makeRootsChange(EmptyRunnable.INSTANCE, RootsChangeRescanningInfo.RESCAN_DEPENDENCIES_IF_NEEDED)
//        }
        println("reload project: ${project.name} stdlib: ${explicitPathToStdlib} dir:${stdlibDir}")
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
        var toolchainLocation by string()
        var explicitPathToStdlib by string()
        var testToolPath by string()

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
