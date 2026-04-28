package org.ton.intellij.acton.cli

import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.openapi.project.Project
import com.intellij.util.execution.ParametersListUtil
import org.ton.intellij.acton.settings.actonSettings
import java.nio.file.Path

const val ACTON_EXECUTABLE_NAME: String = "acton"

fun findActonExecutableInPath(): String? = PathEnvironmentVariableUtil.findInPath(ACTON_EXECUTABLE_NAME)?.absolutePath

fun resolveActonExecutable(project: Project): String? {
    val configuredPath = project.actonSettings.actonPath?.trim().orEmpty()
    return configuredPath.ifBlank { findActonExecutableInPath() }
}

data class ActonCommandLine(
    val command: String,
    val workingDirectory: Path,
    val additionalArguments: List<String> = emptyList(),
    val environmentVariables: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT,
) {
    fun toGeneralCommandLine(project: Project): GeneralCommandLine? {
        val actonPath = resolveActonExecutable(project) ?: return null

        val globalEnvs = project.actonSettings.env
        val mergedEnvs = globalEnvs.envs.toMutableMap().apply {
            putAll(environmentVariables.envs)
        }

        val commandLine = GeneralCommandLine()
            .withExePath(actonPath)
            .withWorkDirectory(workingDirectory.toString())
            .withParameters(ParametersListUtil.parse(command))
            .withParameters(additionalArguments)
            .withEnvironment(mergedEnvs)

        // Local setting overrides global setting for parent envs
        val passParentEnvs = if (environmentVariables != EnvironmentVariablesData.DEFAULT) {
            environmentVariables.isPassParentEnvs
        } else {
            globalEnvs.isPassParentEnvs
        }

        if (!passParentEnvs) {
            commandLine.withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.NONE)
        }

        return commandLine
    }

    companion object {
        fun forProject(
            project: Project,
            command: String,
            workingDirectory: Path,
            additionalArguments: List<String> = emptyList(),
        ): ActonCommandLine = ActonCommandLine(
            command = command,
            workingDirectory = workingDirectory,
            additionalArguments = additionalArguments,
        )
    }
}
