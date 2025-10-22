package org.ton.intellij.tolk.action

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import org.ton.intellij.tolk.ide.configurable.TolkProjectConfigurable
import org.ton.intellij.tolk.ide.configurable.tolkSettings
import org.ton.intellij.tolk.psi.TolkFunction

class TolkRunTestAction : AnAction() {
    private var currentFunction: TolkFunction? = null

    override fun actionPerformed(e: AnActionEvent) {
        val function = currentFunction ?: return
        runTest(function)
    }

    fun runTest(function: TolkFunction) {
        currentFunction = function
        val project = function.project
        val testToolPath = project.tolkSettings.testToolPath

        if (testToolPath.isNullOrBlank()) {
            val configureAction = object : NotificationAction("Configure") {
                override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, TolkProjectConfigurable::class.java)
                }
            }

            showNotification(
                project,
                "Test tool not configured",
                "Please configure the test tool path in Tolk settings",
                NotificationType.WARNING,
                configureAction
            )
            return
        }

        val file = function.containingFile.virtualFile ?: return
        val functionName = function.name ?: return

        runTestTool(project, testToolPath, file, functionName)
    }

    private fun runTestTool(project: Project, toolPath: String, file: VirtualFile, functionName: String) {
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val commandLine = GeneralCommandLine()
                    .withExePath(toolPath)
                    .withParameters("test", file.path, "--filter", functionName.removeSurrounding("`"))
                    .withWorkDirectory(project.basePath)

                val handler = OSProcessHandler(commandLine)
                handler.addProcessListener(object : ProcessAdapter() {
                    override fun processTerminated(event: ProcessEvent) {
                        val exitCode = event.exitCode
                        val message = if (exitCode == 0) {
                            "Test '$functionName' passed"
                        } else {
                            "Test '$functionName' failed with exit code $exitCode"
                        }

                        showNotification(
                            project,
                            "Test Result",
                            message,
                            if (exitCode == 0) NotificationType.INFORMATION else NotificationType.ERROR
                        )
                    }

                    override fun onTextAvailable(event: ProcessEvent, outputType: Key<Any>) {
                        println(event.text)
                        // Optionally handle test output here
                    }
                })

                handler.startNotify()
            } catch (e: Exception) {
                showNotification(
                    project,
                    "Test execution failed",
                    "Error running test: ${e.message}",
                    NotificationType.ERROR
                )
            }
        }
    }

    private fun showNotification(project: Project, title: String, content: String, type: NotificationType, vararg actions: NotificationAction) {
        ApplicationManager.getApplication().invokeLater {
            val notification = Notification(
                "Tolk Test Runner",
                title,
                content,
                type
            )
            actions.forEach { notification.addAction(it) }
            Notifications.Bus.notify(notification, project)
        }
    }
}
