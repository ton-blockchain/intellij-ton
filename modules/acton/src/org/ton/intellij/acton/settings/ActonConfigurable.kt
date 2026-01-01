package org.ton.intellij.acton.settings

import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.Alarm
import org.ton.intellij.util.pathToExecutableTextField
import javax.swing.JLabel

class ActonConfigurable(val project: Project) : BoundConfigurable("Acton"), Disposable {
    private val actonPathField = pathToExecutableTextField(this, "Choose Acton Executable")
    private val actonVersionLabel = JLabel()
    private val environmentVariables = EnvironmentVariablesComponent()
    private val alarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)

    override fun dispose() {
        Disposer.dispose(alarm)
    }

    override fun createPanel(): DialogPanel = panel {
        row("Toolchain location:") {
            cell(actonPathField).align(AlignX.FILL)
        }
        row("Toolchain version:") {
            cell(actonVersionLabel)
        }
        row(environmentVariables.label) {
            cell(environmentVariables).align(AlignX.FILL)
        }

        val settings = project.actonSettings
        onApply {
            settings.actonPath = actonPathField.text.ifBlank { null }
            settings.env = environmentVariables.envData
        }
        onReset {
            val savedPath = settings.actonPath
            actonPathField.text = savedPath ?: findActonInPath() ?: ""
            environmentVariables.envData = settings.env
            
            val cachedVersion = settings.actonVersion
            if (cachedVersion != null) {
                actonVersionLabel.text = cachedVersion
                actonVersionLabel.foreground = JBColor.foreground()
            } else {
                actonVersionLabel.text = "Checking..."
            }
            updateVersion()
        }
        onIsModified {
            actonPathField.text != (settings.actonPath ?: "") ||
            environmentVariables.envData != settings.env
        }

        actonPathField.textField.addActionListener { updateVersion() }
        actonPathField.textField.addFocusListener(object : java.awt.event.FocusAdapter() {
            override fun focusLost(e: java.awt.event.FocusEvent?) {
                updateVersion()
            }
        })
    }

    private fun findActonInPath(): String? {
        return PathEnvironmentVariableUtil.findInPath("acton")?.absolutePath
    }

    private fun updateVersion() {
        val path = actonPathField.text.ifBlank { findActonInPath() ?: "acton" }
        alarm.cancelAllRequests()
        alarm.addRequest({
            val version = try {
                val commandLine = GeneralCommandLine(path, "--version")
                val handler = CapturingProcessHandler(commandLine)
                val output = handler.runProcess(2000)
                if (output.exitCode == 0) {
                    output.stdout.trim()
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }

            invokeLater(ModalityState.any()) {
                if (version == null) {
                    actonVersionLabel.text = "Unknown"
                    actonVersionLabel.foreground = JBColor.RED
                } else {
                    actonVersionLabel.text = version
                    actonVersionLabel.foreground = JBColor.foreground()
                    project.actonSettings.actonVersion = version
                }
            }
        }, 200)
    }
}
