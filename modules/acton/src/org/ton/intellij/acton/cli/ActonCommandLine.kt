package org.ton.intellij.acton.cli

import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.openapi.project.Project
import com.intellij.util.execution.ParametersListUtil
import org.ton.intellij.acton.settings.actonSettings
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name

const val ACTON_EXECUTABLE_NAME: String = "acton"

fun findActonExecutableInPath(): String? = PathEnvironmentVariableUtil.findInPath(ACTON_EXECUTABLE_NAME)?.absolutePath

fun findActonExecutableInDefaultInstall(): String? {
    defaultInstallExecutableCandidates()
        .firstOrNull(::isExecutable)
        ?.let { return it.toString() }

    return findLatestVersionedActonExecutable()?.toString()
}

fun findActonExecutable(): String? = findActonExecutableInPath() ?: findActonExecutableInDefaultInstall()

fun resolveActonExecutable(project: Project): String? {
    val configuredPath = project.actonSettings.actonPath?.trim().orEmpty()
    if (configuredPath.isNotBlank() && isResolvableExecutable(configuredPath)) {
        return configuredPath
    }

    return findActonExecutable()
}

private val DEFAULT_ACTON_HOME: Path = Path.of(System.getProperty("user.home"), ".acton")
private val DEFAULT_ACTON_BIN_DIR: Path = DEFAULT_ACTON_HOME.resolve("bin")

private fun defaultInstallExecutableCandidates(): Sequence<Path> = sequenceOf(
    DEFAULT_ACTON_BIN_DIR.resolve(ACTON_EXECUTABLE_NAME),
    DEFAULT_ACTON_HOME.resolve(ACTON_EXECUTABLE_NAME),
)

private fun findLatestVersionedActonExecutable(): Path? {
    if (!Files.isDirectory(DEFAULT_ACTON_BIN_DIR)) return null

    return Files.list(DEFAULT_ACTON_BIN_DIR).use { files ->
        files
            .filter { it.name.startsWith("$ACTON_EXECUTABLE_NAME-") }
            .filter(::isExecutable)
            .max(Comparator.comparingLong { Files.getLastModifiedTime(it).toMillis() })
            .orElse(null)
    }
}

private fun isExecutable(path: Path): Boolean = Files.isRegularFile(path) && Files.isExecutable(path)

private fun isResolvableExecutable(pathOrCommand: String): Boolean {
    PathEnvironmentVariableUtil.findInPath(pathOrCommand)?.let { return true }

    return try {
        isExecutable(Path.of(pathOrCommand))
    } catch (_: Exception) {
        false
    }
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
