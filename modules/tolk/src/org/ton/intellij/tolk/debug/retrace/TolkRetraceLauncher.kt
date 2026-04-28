@file:Suppress("DEPRECATION")

package org.ton.intellij.tolk.debug.retrace

import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages
import org.ton.intellij.acton.cli.ActonToml
import org.ton.intellij.acton.runconfig.ActonCommandConfiguration
import org.ton.intellij.acton.runconfig.ActonCommandConfigurationType
import java.nio.file.Path

object TolkRetraceLauncher {
    fun launch(
        project: Project,
        transactionHash: String,
        network: String = "",
        contractId: String? = null,
        workingDirectory: Path? = null,
    ): Boolean {
        val actonToml = ActonToml.find(project)
        val resolvedWorkingDirectory =
            workingDirectory ?: actonToml?.workingDir ?: project.guessProjectDir()?.toNioPath()
        if (resolvedWorkingDirectory == null) {
            Messages.showErrorDialog(project, "Cannot determine Acton working directory", "Tolk Retrace")
            return false
        }

        val runManager = RunManager.getInstance(project)
        val settings = runManager.createConfiguration(
            "Retrace ${transactionHash.take(12)}",
            ActonCommandConfigurationType.getInstance().factory,
        )
        val configuration = settings.configuration as ActonCommandConfiguration
        configuration.command = "retrace"
        configuration.retraceTransactionHash = transactionHash.trim()
        configuration.retraceContractId = contractId?.trim().orEmpty()
        configuration.retraceNetwork = network.takeIf { it.isNotBlank() } ?: inferDefaultNetwork(actonToml)
        configuration.workingDirectory = resolvedWorkingDirectory

        runManager.addConfiguration(settings)
        runManager.selectedConfiguration = settings
        ExecutionUtil.runConfiguration(settings, DefaultDebugExecutor.getDebugExecutorInstance())
        return true
    }

    fun chooseContractId(project: Project, actonToml: ActonToml?): String? {
        val contractIds = actonToml?.getContractIds().orEmpty().filter { it.isNotBlank() }
        if (contractIds.size == 1) {
            return contractIds.single()
        }
        if (contractIds.isNotEmpty()) {
            val selectedIndex = Messages.showChooseDialog(
                project,
                "Select contract from Acton.toml",
                "Tolk Retrace",
                null,
                contractIds.toTypedArray(),
                contractIds.first(),
            )
            return contractIds.getOrNull(selectedIndex)
        }

        return Messages.showInputDialog(
            project,
            "Contract name from Acton.toml",
            "Tolk Retrace",
            null,
            "",
            object : InputValidator {
                override fun checkInput(inputString: String?): Boolean = !inputString.isNullOrBlank()

                override fun canClose(inputString: String?): Boolean = checkInput(inputString)
            },
        )?.trim()
    }

    fun inferDefaultNetwork(actonToml: ActonToml?): String {
        val scripts = actonToml?.getScripts()?.values.orEmpty()
        val networks = scripts.mapNotNull { script ->
            NETWORK_REGEX.find(script)?.groupValues?.get(1)
        }.toSet()
        return if (networks.size == 1) networks.single() else ""
    }

    private val NETWORK_REGEX = Regex("""(?:^|\s)--net\s+(testnet|mainnet)(?:\s|$)""")
}
