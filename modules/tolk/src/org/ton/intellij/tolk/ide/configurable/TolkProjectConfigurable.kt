package org.ton.intellij.tolk.ide.configurable

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.Alarm
import org.ton.intellij.tolk.TolkBundle
import org.ton.intellij.tolk.toolchain.TolkToolchain
import org.ton.intellij.tolk.toolchain.flavor.TolkToolchainFlavor
import org.ton.intellij.util.pathToDirectoryTextField
import javax.swing.JLabel
import kotlin.io.path.pathString

class TolkProjectConfigurable(
    val project: Project,
) : BoundConfigurable("Tolk"), Configurable.NoScroll, Disposable {

    data class Data(
        val toolchain: TolkToolchain?,
        val explicitPathToStdlib: String?,
    )

    private val pathToToolchainComboBox = TolkToolchainPathChoosingComboBox { update() }
    private val pathToStdlibField = pathToDirectoryTextField(this, TolkBundle["settings.tolk.toolchain.stdlib.dialog.title"])
    private val toolchainVersion = JLabel()
    private val alarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)

    var data: Data
        get() {
            val toolchain = pathToToolchainComboBox.selectedPath?.let { TolkToolchain.fromPath(it.pathString) }
            var explicitPathToStdlib = pathToStdlibField.text.ifBlank { null }
            if (explicitPathToStdlib != null && toolchain?.stdlibDir == null) {
                explicitPathToStdlib = null
            }
            return Data(
                toolchain = toolchain,
                explicitPathToStdlib = explicitPathToStdlib
            )
        }
    set(value) {
        pathToToolchainComboBox.selectedPath = value.toolchain?.homePath?.toNioPathOrNull()
        pathToStdlibField.text = value.explicitPathToStdlib ?: ""
        update()
    }

    override fun createPanel(): DialogPanel = panel {
        row(TolkBundle["settings.tolk.toolchain.location.label"]) {
            cell(pathToToolchainComboBox).align(AlignX.FILL)
        }
        row(TolkBundle["settings.tolk.toolchain.version.label"]) {
            cell(toolchainVersion)
        }
        row(TolkBundle["settings.tolk.toolchain.stdlib.label"]) {
            cell(pathToStdlibField).align(AlignX.FILL)
        }

        val setting = project.tolkSettings
        onApply {
            setting.state.apply {
                toolchain = data.toolchain
                explicitPathToStdlib = data.explicitPathToStdlib
            }
        }
        onReset {
            val currentData = data
            val newData = Data(
                toolchain = setting.toolchain ?: project.let {
                    TolkToolchain.suggest(project)
                },
                explicitPathToStdlib = setting.explicitPathToStdlib
            )
            if (currentData != newData) {
                data = newData
            }
        }
        onIsModified {
            val data = data
            data.toolchain?.homePath != setting.toolchain?.homePath || data.explicitPathToStdlib != setting.explicitPathToStdlib
        }

        pathToToolchainComboBox.addToolchainsAsync {
            TolkToolchainFlavor.getApplicableFlavors().flatMap { it.suggestHomePaths(project) }.distinct()
        }
    }

    private fun update() {
        val pathToToolchain = pathToToolchainComboBox.selectedPath
        alarm.cancelAllRequests()
        alarm.addRequest({
            val toolchain = pathToToolchain?.let {  TolkToolchain.fromPath(it) }
            val hasStdlib = toolchain != null && toolchain.stdlibDir != null
            val version = toolchain?.version
            invokeLater(ModalityState.any()) {
                pathToStdlibField.isEditable = !hasStdlib
                pathToStdlibField.setButtonEnabled(!hasStdlib)
                if (hasStdlib) {
                    pathToStdlibField.text = toolchain.stdlibDir?.path ?: ""
                }
                if (version == null) {
                    toolchainVersion.text = TolkBundle["settings.tolk.toolchain.version.unknown"]
                    toolchainVersion.foreground = JBColor.RED
                } else {
                    toolchainVersion.text = version
                    toolchainVersion.foreground = JBColor.foreground()
                }
            }
        }, 200)
    }


    override fun dispose() {
        Disposer.dispose(pathToToolchainComboBox)
    }
}
