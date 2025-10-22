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
import org.ton.intellij.util.pathToExecutableTextField
import javax.swing.JLabel
import kotlin.io.path.pathString

class TolkProjectConfigurable(
    val project: Project,
) : BoundConfigurable("Tolk"), Configurable.NoScroll, Disposable {

    data class Data(
        val toolchain: TolkToolchain?,
        val explicitPathToStdlib: String?,
        val testToolPath: String?,
    )

    private val pathToToolchainComboBox = TolkToolchainPathChoosingComboBox { update() }
    private val pathToStdlibField = pathToDirectoryTextField(this, TolkBundle["settings.tolk.toolchain.stdlib.dialog.title"])
    private val pathToTestToolField = pathToExecutableTextField(this, "Choose test tool executable")
    private val toolchainVersion = JLabel()
    private val alarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)

    var data: Data
        get() {
            val toolchain = pathToToolchainComboBox.selectedPath?.let { TolkToolchain.fromPath(it.pathString) }
            val explicitPathToStdlib = pathToStdlibField.text.ifBlank { null }
            val testToolPath = pathToTestToolField.text.ifBlank { null }
            return Data(
                toolchain = toolchain,
                explicitPathToStdlib = explicitPathToStdlib,
                testToolPath = testToolPath
            )
        }
    set(value) {
        pathToToolchainComboBox.selectedPath = value.toolchain?.homePath?.toNioPathOrNull()
        pathToStdlibField.text = value.explicitPathToStdlib ?: ""
        pathToTestToolField.text = value.testToolPath ?: ""
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
        row("Test tool path:") {
            cell(pathToTestToolField).align(AlignX.FILL)
        }

        val setting = project.tolkSettings
        onApply {
            setting.toolchain = data.toolchain ?: TolkToolchain.NULL
            setting.explicitPathToStdlib = data.explicitPathToStdlib
            setting.testToolPath = data.testToolPath
        }
        onReset {
            val currentData = data
            val newData = Data(
                toolchain = setting.toolchain,
                explicitPathToStdlib = setting.explicitPathToStdlib,
                testToolPath = setting.testToolPath
            )
            if (currentData != newData) {
                data = newData
            }
        }
        onIsModified {
            val data = data
            (data.toolchain?.homePath ?: "") != setting.toolchain.homePath ||
            data.explicitPathToStdlib != setting.explicitPathToStdlib ||
            data.testToolPath != setting.testToolPath
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
        Disposer.dispose(alarm)
    }
}
