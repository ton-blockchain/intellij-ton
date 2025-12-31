package org.ton.intellij.tolk.ide.toml

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlTableHeader
import org.toml.lang.psi.ext.name
import org.ton.intellij.acton.runconfig.ActonCommandConfiguration
import org.ton.intellij.acton.runconfig.ActonCommandConfigurationType
import java.nio.file.Paths

class TolkActonTomlLineMarkerProvider : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element.containingFile.name != "Acton.toml") return null
        if (element !is TomlKeySegment) return null

        val header = element.parent?.parent as? TomlTableHeader ?: return null
        val segments = header.key?.segments ?: return null
        if (segments.size != 2) return null
        if (segments[0].name != "contracts") return null
        if (segments[1] != element) return null

        val contractName = element.name ?: return null
        val actions = arrayOf(TolkBuildContractAction(contractName))

        return Info(
            AllIcons.Actions.Rebuild,
            { "Build $contractName" },
            *actions
        )
    }

    private class TolkBuildContractAction(private val contractName: String) : AnAction("Build $contractName", null, AllIcons.Actions.Compile) {
        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project ?: return
            val file = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE) ?: return
            val workingDir = file.parent?.toNioPath() ?: return

            val runManager = RunManager.getInstance(project)
            val configurationName = "Build $contractName"
            val settings = runManager.createConfiguration(configurationName, ActonCommandConfigurationType.getInstance().factory)
            val configuration = settings.configuration as ActonCommandConfiguration
            
            configuration.command = "build"
            configuration.workingDirectory = workingDir
            configuration.buildContractId = contractName
            configuration.parameters = "--info"
            
            runManager.addConfiguration(settings)
            runManager.selectedConfiguration = settings
            
            ExecutionUtil.runConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance())
        }
    }
}
