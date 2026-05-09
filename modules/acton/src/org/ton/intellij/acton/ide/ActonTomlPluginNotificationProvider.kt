package org.ton.intellij.acton.ide

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationPanel.Status
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import org.ton.intellij.acton.ActonBundle
import java.util.function.Function
import javax.swing.JComponent

class ActonTomlPluginNotificationProvider : EditorNotificationProvider {
    override fun collectNotificationData(
        project: Project,
        file: VirtualFile,
    ): Function<in FileEditor, out JComponent?>? {
        if (isTomlPluginInstalled()) return null
        if (file.name != "Acton.toml" && (file.extension != "tolk" || !hasNearestActonToml(project, file))) return null

        return Function { fileEditor ->
            EditorNotificationPanel(fileEditor, Status.Error).apply {
                text = ActonBundle.message("notification.acton.toml.plugin.missing")
                createActionLabel(ActonBundle.message("notification.acton.toml.plugin.action.plugins")) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, "plugins")
                }
            }
        }
    }
}

class ActonTomlPluginNotificationStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        if (isTomlPluginInstalled() || !hasActonToml(project)) return

        ApplicationManager.getApplication().invokeLater {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Acton Setup")
                .createNotification(
                    ActonBundle.message("notification.acton.toml.plugin.title"),
                    ActonBundle.message("notification.acton.toml.plugin.missing"),
                    NotificationType.ERROR,
                )
                .addAction(
                    NotificationAction.createSimple(
                        ActonBundle.message("notification.acton.toml.plugin.action.plugins"),
                    ) {
                        ShowSettingsUtil.getInstance().showSettingsDialog(project, "plugins")
                    },
                )
                .notify(project)

            EditorNotifications.getInstance(project).updateAllNotifications()
        }
    }
}
