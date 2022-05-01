package org.ton.intellij.toncli.project.settings.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.ui.layout.LayoutBuilder
import org.ton.intellij.TonBundle
import org.ton.intellij.UiDebouncer
import org.ton.intellij.toncli.project.TonToolchainPathChoosingComboBox
import org.ton.intellij.toncli.toolchain.TonToolchainBase
import org.ton.intellij.toncli.toolchain.TonToolchainProvider
import org.ton.intellij.toncli.toolchain.tools.toncli
import java.awt.BorderLayout
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class TonProjectSettingsPanel(
        private val toncliProjectDir: Path = Paths.get("."),
        private val updateListener: (() -> Unit)? = null
) : Disposable {
    data class Data(
            val toolchain: TonToolchainBase?,
            val explicitPathToStdlib: String?
    )

    override fun dispose() {
        Disposer.dispose(pathToToolchainComboBox)
    }

    private val versionUpdateDebouncer = UiDebouncer(this)

    private val pathToToolchainComboBox = TonToolchainPathChoosingComboBox { update() }

    private val toolchainVersion = JLabel()

    fun attachTo(layout: LayoutBuilder) = with(layout) {
        row(TonBundle.message("settings.ton.toolchain.location.label")) { wrapComponent(pathToToolchainComboBox)(growX, pushX) }
        row(TonBundle.message("settings.ton.toolchain.version.label")) { toolchainVersion() }
    }

    private fun update() {
        val pathToToolchain = pathToToolchainComboBox.selectedPath
        versionUpdateDebouncer.run(
                onPooledThread = {
                    val toolchain = pathToToolchain?.let { TonToolchainProvider.getToolchain(it) }
                    val toncli = toolchain?.toncli()
                    val toncliVersion: String? = toncli?.queryVersion()
//                    val rustc = toolchain?.rustc()
//                    val rustup = toolchain?.rustup
//                    val rustcVersion = rustc?.queryVersion()?.semver
//                    val stdlibLocation = rustc?.getStdlibFromSysroot(cargoProjectDir)?.presentableUrl
//                    Triple(rustcVersion, stdlibLocation, rustup != null)
                    toncliVersion
                },
                onUiThread = { toncliVersion ->
//                    downloadStdlibLink.isVisible = hasRustup && stdlibLocation == null
//
//                    pathToStdlibField.isEditable = !hasRustup
//                    pathToStdlibField.setButtonEnabled(!hasRustup)
//                    if (stdlibLocation != null && (pathToStdlibField.text.isBlank() || hasRustup) ||
//                            !isStdlibLocationCompatible(pathToToolchain?.toString().orEmpty(), pathToStdlibField.text)) {
//                        pathToStdlibField.text = stdlibLocation.orEmpty()
//                    }
//                    fetchedSysroot = stdlibLocation
//
                    if (toncliVersion == null) {
                        toolchainVersion.text = TonBundle.message("settings.ton.toolchain.not.applicable.version.text")
                        toolchainVersion.foreground = JBColor.RED
                    } else {
                        toolchainVersion.text = toncliVersion
                        toolchainVersion.foreground = JBColor.foreground()
                    }
                    updateListener?.invoke()
                }
        )
    }
}

private fun String.blankToNull(): String? = ifBlank { null }

private fun wrapComponent(component: JComponent): JComponent =
        JPanel(BorderLayout()).apply {
            add(component, BorderLayout.NORTH)
        }