package org.ton.intellij.toncli.tools

import com.intellij.execution.configurations.GeneralCommandLine
import org.ton.intellij.GeneralCommandLine
import org.ton.intellij.toncli.toolchain.TonToolchainBase
import org.ton.intellij.withWorkDirectory
import java.nio.file.Path

abstract class TonTool(
        toolName: String,
        val toolchain: TonToolchainBase
) {
    open val executable: Path = toolchain.pathToExecutable(toolName)

    protected fun createBaseCommandLine(
            vararg parameters: String,
            workingDirectory: Path? = null,
            environment: Map<String, String> = emptyMap()
    ): GeneralCommandLine = createBaseCommandLine(
            parameters.toList(),
            workingDirectory = workingDirectory,
            environment = environment
    )

    protected open fun createBaseCommandLine(
            parameters: List<String>,
            workingDirectory: Path? = null,
            environment: Map<String, String> = emptyMap()
    ): GeneralCommandLine = GeneralCommandLine(executable)
            .withWorkDirectory(workingDirectory)
            .withParameters(parameters)
            .withEnvironment(environment)
            .withCharset(Charsets.UTF_8)
            .also { toolchain.patchCommandLine(it) }
}