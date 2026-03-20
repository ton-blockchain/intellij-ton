package org.ton.intellij.tolk.debug.retrace

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.RunConfigurationWithSuppressedDefaultRunAction
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.platform.dap.DapStartRequest
import org.jdom.Element
import org.ton.intellij.acton.cli.ActonCommandLine
import org.ton.intellij.acton.cli.ActonToml
import org.ton.intellij.tolk.debug.TolkRetraceDebugAdapter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class TolkRetraceConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String
) : LocatableConfigurationBase<TolkRetraceRunState>(project, factory, name),
    RunConfigurationWithSuppressedDefaultRunAction {

    var transactionHash: String = ""
    var contractId: String = ""
    var network: String = ""
    var workingDirectory: Path? = ActonToml.find(project)?.workingDir

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return TolkRetraceRunState(environment, this)
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return TolkRetraceConfigurationEditor(project)
    }

    override fun suggestedName(): String? {
        val hash = transactionHash.trim()
        return if (hash.isNotEmpty()) {
            "Retrace ${hash.take(12)}"
        } else {
            null
        }
    }

    override fun checkConfiguration() {
        val workingDir = workingDirectory ?: throw RuntimeConfigurationError("Working directory is not set")
        if (!Files.isDirectory(workingDir)) {
            throw RuntimeConfigurationError("Working directory does not exist: $workingDir")
        }
        if (transactionHash.isBlank()) {
            throw RuntimeConfigurationError("Transaction hash is required")
        }
        if (contractId.isBlank()) {
            throw RuntimeConfigurationError("Contract id is required")
        }
        if (network.isNotBlank() && network != "mainnet" && network != "testnet") {
            throw RuntimeConfigurationError("Network must be either mainnet or testnet")
        }
        if (ActonCommandLine("retrace", workingDir).toGeneralCommandLine(project) == null) {
            throw RuntimeConfigurationError("Cannot find acton executable")
        }
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.setAttribute("transactionHash", transactionHash)
        element.setAttribute("contractId", contractId)
        element.setAttribute("network", network)
        workingDirectory?.let { element.setAttribute("workingDirectory", it.toString()) }
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        transactionHash = element.getAttributeValue("transactionHash") ?: ""
        contractId = element.getAttributeValue("contractId") ?: ""
        network = element.getAttributeValue("network") ?: ""
        workingDirectory = element.getAttributeValue("workingDirectory")?.let { Paths.get(it) }
    }

    companion object {
        val ADAPTER_ID = TolkRetraceDebugAdapter
        val REQUEST: DapStartRequest = DapStartRequest.Launch
        fun arguments(): Map<String, Any> = emptyMap()
    }
}
