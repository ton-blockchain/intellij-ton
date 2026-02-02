package org.ton.intellij.acton.ide

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.startup.ProjectActivity
import org.ton.intellij.acton.cli.ActonCommandLine
import org.ton.intellij.acton.settings.actonApplicationSettings
import org.ton.intellij.acton.settings.actonSettings

class ActonUpdateActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        if (actonApplicationSettings.disableUpdateChecks) return

        val updateInfo = checkUpdate(project) ?: return
        if (!updateInfo.updateAvailable || !updateInfo.success) return

        val settings = project.actonSettings
        if (settings.skipUpdateVersion == updateInfo.latestVersion) return

        ApplicationManager.getApplication().invokeLater {
            showUpdateNotification(project, updateInfo)
        }
    }

    private fun checkUpdate(project: Project): UpdateInfo? {
        val commandLine = ActonCommandLine(
            command = "up",
            workingDirectory = project.guessProjectDir()?.toNioPath() ?: return null,
            additionalArguments = listOf("--check"),
            environmentVariables = EnvironmentVariablesData.DEFAULT
        ).toGeneralCommandLine(project)

        val handler = CapturingProcessHandler(commandLine)
        val output = handler.runProcess(10000)
        if (output.exitCode != 0) return null

        return try {
            Gson().fromJson(output.stdout, UpdateInfo::class.java)
        } catch (e: Exception) {
            LOG.error("Failed to parse update info", e)
            null
        }
    }

    private fun showUpdateNotification(project: Project, info: UpdateInfo) {
        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup("Acton Update")
            .createNotification(
                "Acton update available",
                "A new version of Acton is available: ${info.latestVersion} (current: ${info.currentVersion})",
                NotificationType.INFORMATION
            )

        notification.addAction(NotificationAction.createSimple("Update now") {
            notification.expire()
            runUpdate(project)
        })

        notification.addAction(NotificationAction.createSimple("Don't show for this version") {
            project.actonSettings.skipUpdateVersion = info.latestVersion
            notification.expire()
        })

        notification.addAction(NotificationAction.createSimple("Never show again") {
            actonApplicationSettings.disableUpdateChecks = true
            notification.expire()
        })

        notification.notify(project)
    }

    private fun runUpdate(project: Project) {
        var commandLine = ActonCommandLine(
            command = "up",
            workingDirectory = project.guessProjectDir()?.toNioPath() ?: return,
            additionalArguments = listOf("--yes")
        ).toGeneralCommandLine(project)

        commandLine = PtyCommandLine(commandLine)
            .withInitialColumns(PtyCommandLine.MAX_COLUMNS)
            .withConsoleMode(true)

        val consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project)
        val console = consoleBuilder.console
        val handler = KillableColoredProcessHandler(commandLine)

        val executor = DefaultRunExecutor.getRunExecutorInstance()
        val descriptor = RunContentDescriptor(console, handler, console.component, "Acton Update")

        console.attachToProcess(handler)
        RunContentManager.getInstance(project).showRunContent(executor, descriptor)
        handler.startNotify()
    }

    data class UpdateInfo(
        val success: Boolean,
        @SerializedName("current_version")
        val currentVersion: String,
        @SerializedName("latest_version")
        val latestVersion: String,
        @SerializedName("update_available")
        val updateAvailable: Boolean,
    )

    companion object {
        private val LOG = logger<ActonUpdateActivity>()
    }
}
