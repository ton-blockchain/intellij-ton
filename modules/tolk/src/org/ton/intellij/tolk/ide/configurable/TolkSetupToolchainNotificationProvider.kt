package org.ton.intellij.tolk.ide.configurable

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationPanel.Status
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import org.ton.intellij.tolk.TolkBundle
import org.ton.intellij.tolk.ide.configurable.TolkProjectSettingsService.TolkSettingsListener
import org.ton.intellij.tolk.psi.isTolkFile
import org.ton.intellij.tolk.toolchain.TolkToolchain
import org.ton.intellij.tolk.toolchain.guessAndSetupTolkProject
import java.util.function.Function
import javax.swing.JComponent

class TolkSetupToolchainNotificationProvider(
    private val project: Project
) : EditorNotificationProvider {
    init {
        project.messageBus.connect().apply {
            subscribe(TolkSettingsListener.TOPIC, object : TolkSettingsListener {
                override fun tolkSettingsChanged() {
                    updateAllNotifications()
                }
            })
        }
    }

    override fun collectNotificationData(
        project: Project,
        file: VirtualFile
    ): Function<in FileEditor, out JComponent?>? {
        if (!file.isTolkFile) return null
        if (isNotificationDisabled()) return null
        if (guessAndSetupTolkProject(project)) return null

        val toolchain = project.tolkSettings.toolchain
        if (toolchain == TolkToolchain.NULL) {
            return createToolchainNotification(project)
        }

        return null
    }

    private fun createToolchainNotification(
        project: Project,
    ) : Function<in FileEditor, out JComponent?> = Function { fileEditor ->
        EditorNotificationPanel(fileEditor, Status.Warning).apply {
            text = TolkBundle.message("notification.no.toolchain.configured")
            createActionLabel(TolkBundle.message("notification.action.set.up.toolchain.text")) {
                project.tolkSettings.configureToolchain()
            }
            createActionLabel(TolkBundle.message("notification.action.do.not.show.again.text")) {
                disableNotification()
                updateAllNotifications()
            }
        }
    }

    private fun disableNotification() {
        PropertiesComponent.getInstance(project).setValue(NOTIFICATION_STATUS_KEY, true)
    }

    private fun isNotificationDisabled(): Boolean =
        PropertiesComponent.getInstance(project).getBoolean(NOTIFICATION_STATUS_KEY)

    private fun updateAllNotifications() {
        EditorNotifications.getInstance(project).updateAllNotifications()
    }

    private companion object {
        private const val NOTIFICATION_STATUS_KEY = "org.ton.tolk.hideToolchainNotifications"
    }
}
