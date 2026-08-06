package org.ton.intellij.acton.ide

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationPanel.Status
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import org.ton.intellij.acton.ActonBundle
import java.util.function.Function
import javax.swing.JComponent

class ActonWrapperFreshnessNotificationProvider : EditorNotificationProvider {
    override fun collectNotificationData(
        project: Project,
        file: VirtualFile,
    ): Function<in FileEditor, out JComponent?>? {
        val target = findActonWrapperTarget(project, file) ?: return null
        return when (project.actonWrapperFreshness.check(target)) {
            ActonWrapperFreshness.OUTDATED -> Function { fileEditor ->
                EditorNotificationPanel(fileEditor, Status.Warning).apply {
                    text = ActonBundle.message("notification.acton.wrapper.outdated")
                    createActionLabel(ActonBundle.message("notification.acton.wrapper.action.regenerate")) {
                        project.actonWrapperFreshness.update(target)
                        EditorNotifications.getInstance(project).updateNotifications(file)
                    }
                }
            }

            else -> null
        }
    }
}
