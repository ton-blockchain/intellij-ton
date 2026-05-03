package org.ton.intellij.acton.toml

import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vfs.VirtualFile
import org.ton.intellij.acton.ActonIcons
import org.ton.intellij.acton.runconfig.ActonCommandConfiguration
import org.ton.intellij.acton.runconfig.ActonCommandConfigurationType

class ActonInitializeDappAction :
    DumbAwareAction(
        "Initialize dApp",
        "Run acton init --create-dapp",
        ActonIcons.D_APP,
    ) {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = e.project != null && file != null && canInitializeDapp(file)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)?.takeIf(::canInitializeDapp) ?: return
        val workingDir = file.parent?.toNioPath() ?: return

        val runManager = RunManager.getInstance(project)
        val settings = runManager.createConfiguration(
            "Initialize dApp",
            ActonCommandConfigurationType.getInstance().factory,
        )
        val configuration = settings.configuration as ActonCommandConfiguration

        configuration.command = "init"
        configuration.workingDirectory = workingDir
        configuration.parameters = "--create-dapp"

        runManager.addConfiguration(settings)
        runManager.selectedConfiguration = settings

        ExecutionUtil.runConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance())
    }
}

internal fun canInitializeDapp(file: VirtualFile): Boolean {
    if (file.isDirectory || file.name != "Acton.toml") return false

    val parent = file.parent ?: return false
    return parent.findChild("app")?.isDirectory != true
}
