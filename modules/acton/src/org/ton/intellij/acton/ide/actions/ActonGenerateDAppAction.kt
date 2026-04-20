package org.ton.intellij.acton.ide.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import org.ton.intellij.acton.ActonIcons

class ActonGenerateDAppAction : AnAction("Generate dApp", null, ActonIcons.D_APP) {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: e.getData(CommonDataKeys.PSI_FILE)?.virtualFile
        e.presentation.isEnabledAndVisible = e.project != null && virtualFile?.name == "Acton.toml"
    }

    override fun actionPerformed(e: AnActionEvent) = Unit
}
