package org.ton.intellij.tolk.acton

import com.intellij.execution.ExecutionException
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import org.ton.intellij.acton.ActonUtils.stripAnsiColors
import org.ton.intellij.acton.cli.ActonCommand
import org.ton.intellij.acton.cli.ActonCommandLine
import org.ton.intellij.acton.cli.ActonToml
import org.ton.intellij.acton.cli.resolveActonExecutable
import org.ton.intellij.tolk.TolkBundle
import org.ton.intellij.tolk.ide.configurable.tolkSettings

object TolkActonStdlibSetupSupport {
    fun canSetUpStdlib(project: Project): Boolean =
        ActonToml.find(project) != null && resolveActonExecutable(project) != null

    fun addSetUpStdlibAction(panel: EditorNotificationPanel, project: Project) {
        if (!canSetUpStdlib(project)) return

        panel.createActionLabel(TolkBundle.message("notification.action.setup.stdlib.text")) {
            runStdlibInit(project)
        }
    }

    fun showMissingStdlibNotification(project: Project) {
        if (project.tolkSettings.hasStdlib || !canSetUpStdlib(project)) return

        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID)
            .createNotification(
                TolkBundle.message("notification.tolk.stdlib.missing.title"),
                TolkBundle.message("notification.tolk.stdlib.missing.content"),
                NotificationType.WARNING,
            )

        notification.addAction(
            NotificationAction.createSimple(TolkBundle.message("notification.action.run.initial.acton.build.text")) {
                notification.expire()
                runInitialBuild(project)
            },
        )

        notification.notify(project)
    }

    fun runInitialBuild(project: Project) {
        val command = ActonCommand.Build()
        runProjectCommand(project, command, ACTON_BUILD_TIMEOUT_MS) { details ->
            showCommandFailedNotification(
                project,
                TolkBundle.message("notification.tolk.stdlib.build.failed.title"),
                TolkBundle.message("notification.tolk.stdlib.build.failed.content"),
                details,
            )
        }
    }

    fun runStdlibInit(project: Project) {
        val command = ActonCommand.Init(stdlibOnly = true)
        runProjectCommand(project, command, STDLIB_INIT_TIMEOUT_MS) { details ->
            showCommandFailedNotification(
                project,
                TolkBundle.message("notification.tolk.stdlib.setup.failed.title"),
                TolkBundle.message("notification.tolk.stdlib.setup.failed.content"),
                details,
            )
        }
    }

    private fun runProjectCommand(
        project: Project,
        command: ActonCommand,
        timeoutMs: Int,
        onFailure: (String) -> Unit,
    ) {
        val actonToml = ActonToml.find(project) ?: return
        val commandLine = ActonCommandLine(
            command = command.name,
            workingDirectory = actonToml.workingDir,
            additionalArguments = command.getArguments(),
        ).toGeneralCommandLine(project) ?: return

        ApplicationManager.getApplication().executeOnPooledThread {
            val output = try {
                CapturingProcessHandler(commandLine).runProcess(timeoutMs)
            } catch (e: ExecutionException) {
                ApplicationManager.getApplication().invokeLater {
                    onFailure(e.message.orEmpty())
                }
                return@executeOnPooledThread
            }

            ApplicationManager.getApplication().invokeLater {
                if (output.exitCode == 0) {
                    VfsUtil.markDirtyAndRefresh(true, true, true, actonToml.virtualFile.parent)
                    project.tolkSettings.refreshDetectedStdlib()
                    EditorNotifications.getInstance(project).updateAllNotifications()
                } else {
                    val message = stripAnsiColors(output.stderr.ifBlank { output.stdout })
                    onFailure(message)
                }
            }
        }
    }

    private fun showCommandFailedNotification(project: Project, title: String, baseContent: String, details: String) {
        val content = buildString {
            append(baseContent)
            val firstLine = details.trim().lineSequence().firstOrNull()
            if (!firstLine.isNullOrBlank()) {
                append("\n")
                append(firstLine.take(MAX_NOTIFICATION_DETAIL_LENGTH))
            }
        }

        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID)
            .createNotification(
                title,
                content,
                NotificationType.ERROR,
            )
            .notify(project)
    }

    private const val NOTIFICATION_GROUP_ID = "Tolk Toolchain"
    private const val ACTON_BUILD_TIMEOUT_MS = 120_000
    private const val STDLIB_INIT_TIMEOUT_MS = 60_000
    private const val MAX_NOTIFICATION_DETAIL_LENGTH = 300
}
