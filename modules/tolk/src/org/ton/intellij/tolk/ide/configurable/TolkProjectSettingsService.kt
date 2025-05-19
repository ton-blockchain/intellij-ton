package org.ton.intellij.tolk.ide.configurable

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.findFile
import com.intellij.openapi.vfs.findPsiFile
import org.ton.intellij.tolk.psi.TolkFile
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
    var toolchain: TolkToolchain?
        get() = state.toolchainLocation?.let { TolkToolchain.fromPath(it) }
        set(value) {
            state.toolchainLocation = value?.homePath
            defaultImport = null
        }
    var explicitPathToStdlib: String?
        get() = state.explicitPathToStdlib
        set(value) {
            state.explicitPathToStdlib = value
            defaultImport = null
        }
    val stdlibDir: VirtualFile? get() {
        return explicitPathToStdlib?.let {
            it.toNioPathOrNull()?.let {  path ->
                VirtualFileManager.getInstance().findFileByNioPath(path)
            }
        } ?: toolchain?.stdlibDir
    }

    private var defaultImport: TolkFile? = null

    fun getDefaultImport(): TolkFile? {
        val currentDefaultImport = defaultImport
        if (currentDefaultImport == null) {
            val result = stdlibDir?.findFile("common.tolk")?.findPsiFile(project) as? TolkFile
            defaultImport = result
            return result
        }
        return currentDefaultImport
    }

    class TolkProjectSettings : BaseState() {
        var toolchainLocation by string()
        var explicitPathToStdlib by string()

        fun copy() = TolkProjectSettings().also { it.copyFrom(this) }
    }
}
