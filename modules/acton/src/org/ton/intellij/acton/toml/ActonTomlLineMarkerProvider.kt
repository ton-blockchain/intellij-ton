package org.ton.intellij.acton.toml

import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiElement
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlKeyValue
import org.toml.lang.psi.TomlTable
import org.toml.lang.psi.TomlTableHeader
import org.ton.intellij.acton.cli.ActonCommand
import org.ton.intellij.acton.runconfig.ActonCommandConfiguration
import org.ton.intellij.acton.runconfig.ActonCommandConfigurationType

class ActonTomlLineMarkerProvider : RunLineMarkerContributor() {
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
                    arrayOf(TolkBuildContractAction(contractName)),
                ) { "Build $contractName" }
            }

            if (segments.size == 1 && segments[0] == element) {
                return when (segments[0].name) {
                    "test" -> Info(
                        AllIcons.RunConfigurations.TestState.Run,
                        arrayOf(ActonRunTestsAction(), ActonRunTestsWithUiAction()),
                    ) { "Run tests" }

                    "lint" -> Info(
                        AllIcons.General.InspectionsOK,
                        arrayOf(ActonRunLintAction()),
                    ) { "Run linter" }

                    "fmt" -> Info(
                        AllIcons.Actions.Execute,
                        arrayOf(ActonCheckFormattingAction(), ActonFormatProjectAction()),
                    ) { "Run formatter" }

                    else -> null
                }
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
                        arrayOf(TolkRunScriptAction(scriptName)),
                    ) { "Run script $scriptName" }
                }
            }
        }

        return null
    }

    private class ActonRunTestsAction : AnAction("Run All Tests", null, AllIcons.Actions.Execute) {
        override fun actionPerformed(e: AnActionEvent) {
            runConfiguration(e, "Run all tests") {
                command = "test"
                testMode = ActonCommand.Test.TestMode.DIRECTORY
                testTarget = "."
            }
        }
    }

    private class ActonRunTestsWithUiAction : AnAction("Run All Tests with UI", null, AllIcons.Actions.Execute) {
        override fun actionPerformed(e: AnActionEvent) {
            runConfiguration(e, "Run all tests with UI") {
                command = "test"
                testMode = ActonCommand.Test.TestMode.DIRECTORY
                testTarget = "."
                testUi = true
            }
        }
    }

    private class ActonRunLintAction : AnAction("Run Linter", null, AllIcons.General.InspectionsOK) {
        override fun actionPerformed(e: AnActionEvent) {
            runConfiguration(e, "Run linter") {
                command = "check"
            }
        }
    }

    private class ActonCheckFormattingAction : AnAction("Check Formatting", null, AllIcons.General.InspectionsOK) {
        override fun actionPerformed(e: AnActionEvent) {
            runConfiguration(e, "Check formatting") {
                command = "fmt"
                parameters = "--check"
            }
        }
    }

    private class ActonFormatProjectAction : AnAction("Format Project", null, AllIcons.Actions.Execute) {
        override fun actionPerformed(e: AnActionEvent) {
            runConfiguration(e, "Format project") {
                command = "fmt"
            }
        }
    }

    private class TolkBuildContractAction(private val contractName: String) :
        AnAction("Build $contractName", null, AllIcons.Actions.Compile) {
        override fun actionPerformed(e: AnActionEvent) {
            runConfiguration(e, "Build $contractName") {
                command = "build"
                buildContractId = contractName
                parameters = "--info"
            }
        }
    }

    private class TolkRunScriptAction(private val scriptName: String) :
        AnAction("Run $scriptName command", null, AllIcons.Actions.Execute) {
        override fun actionPerformed(e: AnActionEvent) {
            runConfiguration(e, "Run $scriptName command") {
                command = "run"
                runScriptName = scriptName
            }
        }
    }

    private companion object {
        fun runConfiguration(
            e: AnActionEvent,
            configurationName: String,
            configure: ActonCommandConfiguration.() -> Unit,
        ) {
            val project = e.project ?: return
            val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
            val workingDir = file.parent?.toNioPath() ?: return

            val runManager = RunManager.getInstance(project)
            val settings = runManager.createConfiguration(configurationName, ActonCommandConfigurationType.getInstance().factory)
            val configuration = settings.configuration as ActonCommandConfiguration

            configuration.workingDirectory = workingDir
            configuration.configure()

            runManager.addConfiguration(settings)
            runManager.selectedConfiguration = settings

            ExecutionUtil.runConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance())
        }
    }
}
