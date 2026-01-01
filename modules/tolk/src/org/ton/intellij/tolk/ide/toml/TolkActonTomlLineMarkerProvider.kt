package org.ton.intellij.tolk.ide.toml

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.PsiElement
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlKeyValue
import org.toml.lang.psi.TomlTable
import org.toml.lang.psi.TomlTableHeader
import org.ton.intellij.acton.runconfig.ActonCommandConfiguration
import org.ton.intellij.acton.runconfig.ActonCommandConfigurationType

class TolkActonTomlLineMarkerProvider : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element.containingFile.name != "Acton.toml") return null
        if (element !is TomlKeySegment) return null

        // Handle [contracts.name]
        val header = element.parent?.parent as? TomlTableHeader
        if (header != null) {
            val segments = header.key?.segments ?: return null
            if (segments.size == 2 && segments[0].name == "contracts" && segments[1] == element) {
                val contractName = element.name ?: return null
                return Info(
                    AllIcons.Actions.Rebuild,
                    { "Build $contractName" },
                    TolkBuildContractAction(contractName)
                )
            }
        }

        // Handle [scripts] key = "value"
        val keyValue = element.parent?.parent as? TomlKeyValue
        if (keyValue != null) {
            val table = keyValue.parent as? TomlTable
            if (table != null) {
                val headerSegments = table.header.key?.segments ?: return null
                if (headerSegments.size == 1 && headerSegments[0].name == "scripts" && keyValue.key.segments.firstOrNull() == element) {
                    val scriptName = element.name ?: return null
                    return Info(
                        AllIcons.Actions.Execute,
                        { "Run script $scriptName" },
                        TolkRunScriptAction(scriptName)
                    )
                }
            }
        }

        return null
    }

    private class TolkBuildContractAction(private val contractName: String) :
        AnAction("Build $contractName", null, AllIcons.Actions.Compile) {
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

    private class TolkRunScriptAction(private val scriptName: String) : AnAction("Run $scriptName command", null, AllIcons.Actions.Execute) {
        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project ?: return
            val file = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE) ?: return
            val workingDir = file.parent?.toNioPath() ?: return

            val runManager = RunManager.getInstance(project)
            val configurationName = "Run $scriptName command"
            val settings = runManager.createConfiguration(configurationName, ActonCommandConfigurationType.getInstance().factory)
            val configuration = settings.configuration as ActonCommandConfiguration

            configuration.command = "run"
            configuration.workingDirectory = workingDir
            configuration.runScriptName = scriptName

            runManager.addConfiguration(settings)
            runManager.selectedConfiguration = settings

            ExecutionUtil.runConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance())
        }
    }
}
