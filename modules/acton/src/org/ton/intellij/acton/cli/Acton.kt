package org.ton.intellij.acton.cli

import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import java.nio.file.Path

class Acton {
    fun build(
        project: Project,
        workingDirectory: Path,
        contractName: String? = null
    ): ProcessOutput? {
        val args = if (contractName != null) listOf(contractName) else emptyList()
        val commandLine = ActonCommandLine("build", workingDirectory, args)
        return execute(project, commandLine)
    }

    private fun execute(project: Project, commandLine: ActonCommandLine): ProcessOutput? {
        val generalCommandLine = commandLine.toGeneralCommandLine(project)
        return try {
            val handler = CapturingProcessHandler(generalCommandLine)
            ProgressManager.getInstance().runProcessWithProgressSynchronously<ProcessOutput, Exception>(
                { handler.runProcess() },
                "Executing acton ${commandLine.command}",
                true,
                null
            )
        } catch (e: Exception) {
            LOG.error("Failed to execute acton command: ${commandLine.command}", e)
            null
        }
    }

    companion object {
        private val LOG = logger<Acton>()
    }
}
