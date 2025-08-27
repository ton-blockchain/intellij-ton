package org.ton.intellij.boc

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.invokeLater
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JProgressBar

class InstallTonAssemblyActionPanel(
    private val project: Project,
    private val onInstalled: (() -> Unit)? = null,
) : JPanel(BorderLayout()), Disposable {

    private lateinit var installButton: JButton
    private val progressBar = JProgressBar().apply {
        isIndeterminate = true
        isVisible = false
    }
    private val statusLabel = JBLabel("").apply {
        isVisible = false
    }

    private val consoleView: ConsoleView =
        TextConsoleBuilderFactory.getInstance().createBuilder(project)
            .also { it.setViewer(true) }
            .console

    private val consoleBlock = JPanel(BorderLayout()).apply {
        isVisible = false
    }

    init {
        Disposer.register(this, consoleView)
        add(buildUi(), BorderLayout.CENTER)
    }

    private fun buildUi() = panel {
        row { label("TON Assembly (TASM) is not installed").bold() }
        row { text("This will install the <code>ton-assembly</code> package globally using npm.") }
        row {
            installButton = button("Install TASM") { installTonAssembly() }
                .gap(RightGap.SMALL)
                .component

            link("Open documentation") {
                BrowserUtil.browse("https://www.npmjs.com/package/ton-assembly")
            }
        }
        row {
            comment("Command to be executed: <code>npm install -g ton-assembly</code><br/>Please reopen the BOC file after installation.")
        }
        row { cell(progressBar).align(AlignX.FILL).resizableColumn() }
        row { cell(statusLabel) }

        row {
            cell(consoleBlock)
                .align(AlignX.FILL)
                .resizableColumn()
        }.resizableRow()
    }

    private fun showInstallationLogConsole() {
        if (!consoleBlock.isVisible) {
            consoleBlock.add(consoleView.component, BorderLayout.CENTER)
            consoleBlock.isVisible = true
            consoleBlock.revalidate()
            consoleBlock.repaint()
        }
    }

    private fun installTonAssembly() {
        val result = Messages.showYesNoDialog(
            project,
            "This will run the following command:\n\nnpm install -g ton-assembly\n\nContinue?",
            "Install TASM",
            Messages.getQuestionIcon()
        )
        if (result != Messages.YES) return

        invokeLater {
            progressBar.isVisible = true
            statusLabel.text = "Installing ton-assembly…"
            statusLabel.isVisible = true
            installButton.isEnabled = false

            consoleView.clear()
            consoleView.print("\n", ConsoleViewContentType.NORMAL_OUTPUT)
            showInstallationLogConsole()
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            var handler: ProcessHandler?
            try {
                val npmExe = if (SystemInfo.isWindows) "npm.cmd" else "npm"
                val commandLine = GeneralCommandLine(npmExe, "install", "-g", "ton-assembly")

                handler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(commandLine)
                ProcessTerminatedListener.attach(handler)

                consoleView.attachToProcess(handler)

                handler.addProcessListener(object : ProcessAdapter() {
                    override fun processTerminated(event: ProcessEvent) {
                        ApplicationManager.getApplication().invokeLater {
                            progressBar.isVisible = false
                            installButton.isEnabled = true
                            if (event.exitCode == 0) {
                                statusLabel.text = "Installed successfully!"
                                Messages.showInfoMessage(
                                    project,
                                    "TASM installed successfully! File will be reopen automatically.",
                                    "Installation Complete"
                                )

                                onInstalled?.invoke()
                            } else {
                                statusLabel.text = "Installation failed!"
                                Messages.showErrorDialog(
                                    project,
                                    "Failed to install ton-assembly. Please install manually:\n\nnpm install -g ton-assembly",
                                    "Installation Failed"
                                )
                            }
                        }
                    }
                })

                handler.startNotify()
                handler.waitFor()
            } catch (e: Exception) {
                consoleView.print("\nError: ${e.message}\n", ConsoleViewContentType.ERROR_OUTPUT)
                ApplicationManager.getApplication().invokeLater {
                    progressBar.isVisible = false
                    installButton.isEnabled = true
                    statusLabel.text = "Installation error."
                    Messages.showErrorDialog(
                        project,
                        "Error: ${e.message}\n\nPlease install manually: npm install -g ton-assembly",
                        "Installation Error"
                    )
                }
            }
        }
    }

    override fun dispose() {}
}
