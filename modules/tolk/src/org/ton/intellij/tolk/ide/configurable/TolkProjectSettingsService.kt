package org.ton.intellij.tolk.ide.configurable

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import org.ton.intellij.tolk.toolchain.TolkToolchain

val Project.tolkSettings: TolkProjectSettingsService get() = service()

val Project.tolkToolchain: TolkToolchain? get() = tolkSettings.toolchain

@State(
    name = "TolkProjectSettings",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
@Service(Service.Level.PROJECT)
class TolkProjectSettingsService(
    private val project: Project
) : SimplePersistentStateComponent<TolkProjectSettingsService.TolkProjectSettings>(TolkProjectSettings()) {
    var toolchain: TolkToolchain? get() = state.toolchain
        set(value) {
            state.toolchain = value
        }
    val explicitPathToStdlib: String? get() = state.explicitPathToStdlib
    val stdlibDir: VirtualFile? get() {
        return explicitPathToStdlib?.let {
            it.toNioPathOrNull()?.let {  path ->
                VirtualFileManager.getInstance().findFileByNioPath(path)
            }
        } ?: toolchain?.stdlibDir
    }

    class TolkProjectSettings : BaseState() {
        var toolchainLocation by string()
        var explicitPathToStdlib by string()

        var toolchain: TolkToolchain?
            get() = toolchainLocation?.let { TolkToolchain.fromPath(it) }
            set(value) {
                toolchainLocation = value?.homePath
            }

        fun copy() = TolkProjectSettings().also { it.copyFrom(this) }
    }
}
