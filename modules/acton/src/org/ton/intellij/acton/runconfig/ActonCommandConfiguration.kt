package org.ton.intellij.acton.runconfig

import com.intellij.execution.Executor
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import org.jdom.Element
import org.ton.intellij.acton.cli.ActonCommand
import java.nio.file.Path
import java.nio.file.Paths

class ActonCommandConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String,
) : LocatableConfigurationBase<ActonCommandRunState>(project, factory, name) {

    var command: String = "build"
    var workingDirectory: Path? = null
    var parameters: String = ""
    var env: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT
    var emulateTerminal: Boolean = true

    // Build command specific settings
    var buildContractId: String = ""
    var buildClearCache: Boolean = false
    var buildOutDir: String = ""

    // Script command specific settings
    var scriptPath: String = ""
    var scriptClearCache: Boolean = false
    var scriptForkNet: String = ""
    var scriptForkBlockNumber: String = ""
    var scriptApiKey: String = ""
    var scriptBroadcast: Boolean = false
    var scriptBroadcastNet: String = ""
    var scriptExplorer: String = ""
    var scriptDebug: Boolean = false
    var scriptDebugPort: String = ""

    // Test command specific settings
    var testMode: ActonCommand.Test.TestMode = ActonCommand.Test.TestMode.DIRECTORY
    var testTarget: String = ""
    var testFunctionName: String = ""
    var testClearCache: Boolean = false

    fun getActonCommand(): ActonCommand {
        return when (command) {
            "build"  -> ActonCommand.Build(buildContractId, buildClearCache, buildOutDir)
            "script" -> ActonCommand.Script(
                scriptPath, scriptClearCache, scriptForkNet, scriptForkBlockNumber, scriptApiKey,
                scriptBroadcast, scriptBroadcastNet, scriptExplorer, scriptDebug, scriptDebugPort
            )

            "test"   -> ActonCommand.Test(testMode, testTarget, testFunctionName, testClearCache)
            else     -> ActonCommand.Custom(command)
        }
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return ActonCommandRunState(environment, this)
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return ActonCommandConfigurationEditor(project)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.setAttribute("command", command)
        workingDirectory?.let { element.setAttribute("workingDirectory", it.toString()) }
        element.setAttribute("parameters", parameters)
        element.setAttribute("emulateTerminal", emulateTerminal.toString())

        // Build settings
        element.setAttribute("buildContractId", buildContractId)
        element.setAttribute("buildClearCache", buildClearCache.toString())
        element.setAttribute("buildOutDir", buildOutDir)

        // Script settings
        element.setAttribute("scriptPath", scriptPath)
        element.setAttribute("scriptClearCache", scriptClearCache.toString())
        element.setAttribute("scriptForkNet", scriptForkNet)
        element.setAttribute("scriptForkBlockNumber", scriptForkBlockNumber)
        element.setAttribute("scriptApiKey", scriptApiKey)
        element.setAttribute("scriptBroadcast", scriptBroadcast.toString())
        element.setAttribute("scriptBroadcastNet", scriptBroadcastNet)
        element.setAttribute("scriptExplorer", scriptExplorer)
        element.setAttribute("scriptDebug", scriptDebug.toString())
        element.setAttribute("scriptDebugPort", scriptDebugPort)

        // Test settings
        element.setAttribute("testMode", testMode.name)
        element.setAttribute("testTarget", testTarget)
        element.setAttribute("testFunctionName", testFunctionName)
        element.setAttribute("testClearCache", testClearCache.toString())

        env.writeExternal(element)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        command = element.getAttributeValue("command") ?: "build"
        workingDirectory = element.getAttributeValue("workingDirectory")?.let { Paths.get(it) }
        parameters = element.getAttributeValue("parameters") ?: ""
        emulateTerminal = element.getAttributeValue("emulateTerminal")?.toBoolean() ?: true

        // Build settings
        buildContractId = element.getAttributeValue("buildContractId") ?: ""
        buildClearCache = element.getAttributeValue("buildClearCache")?.toBoolean() ?: false
        buildOutDir = element.getAttributeValue("buildOutDir") ?: ""

        // Script settings
        scriptPath = element.getAttributeValue("scriptPath") ?: ""
        scriptClearCache = element.getAttributeValue("scriptClearCache")?.toBoolean() ?: false
        scriptForkNet = element.getAttributeValue("scriptForkNet") ?: ""
        scriptForkBlockNumber = element.getAttributeValue("scriptForkBlockNumber") ?: ""
        scriptApiKey = element.getAttributeValue("scriptApiKey") ?: ""
        scriptBroadcast = element.getAttributeValue("scriptBroadcast")?.toBoolean() ?: false
        scriptBroadcastNet = element.getAttributeValue("scriptBroadcastNet") ?: ""
        scriptExplorer = element.getAttributeValue("scriptExplorer") ?: ""
        scriptDebug = element.getAttributeValue("scriptDebug")?.toBoolean() ?: false
        scriptDebugPort = element.getAttributeValue("scriptDebugPort") ?: ""

        // Test settings
        testMode =
            element.getAttributeValue("testMode")?.let { ActonCommand.Test.TestMode.valueOf(it) } ?: ActonCommand.Test.TestMode.DIRECTORY
        testTarget = element.getAttributeValue("testTarget") ?: ""
        testFunctionName = element.getAttributeValue("testFunctionName") ?: ""
        testClearCache = element.getAttributeValue("testClearCache")?.toBoolean() ?: false

        env = EnvironmentVariablesData.readExternal(element)
    }
}
