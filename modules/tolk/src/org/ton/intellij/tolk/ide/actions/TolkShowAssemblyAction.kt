package org.ton.intellij.tolk.ide.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import org.ton.intellij.tolk.TolkFileType
import org.ton.intellij.tolk.ide.assembly.TolkAssemblyPreviewManager

class TolkShowAssemblyAction : AnAction("Show Assembly") {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        val vFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val enabled = project != null && vFile != null && vFile.fileType == TolkFileType
        e.presentation.isEnabledAndVisible = enabled
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        TolkAssemblyPreviewManager.open(project, vFile)
    }
}
