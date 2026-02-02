package org.ton.intellij.boc

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.execution.process.CapturingProcessAdapter
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessNotCreatedException
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import org.ton.intellij.acton.cli.ActonCommand
import org.ton.intellij.acton.cli.ActonCommandLine
import org.ton.intellij.acton.settings.actonSettings
import java.util.concurrent.TimeUnit
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Base64
import java.util.HexFormat

object BocActonService {
    private val LOG = logger<BocActonService>()

    fun isAvailable(project: Project): Boolean {
        val path = project.actonSettings.actonPath ?: PathEnvironmentVariableUtil.findInPath("acton")?.absolutePath
        return path != null
    }

    fun disassemble(project: Project, bocFilePath: String) = executeActonCommand(project, ActonCommand.Disasm(bocFile = bocFilePath))

    fun disassembleDetailed(project: Project, bocFilePath: String) =
        executeActonCommand(project, ActonCommand.Disasm(bocFile = bocFilePath, showHashes = true, showOffsets = true))

    fun readFileAsHex(path: String): Result<String> = try {
        val bytes = Files.readAllBytes(Paths.get(path))
        val hex = HexFormat.of().formatHex(bytes) // lowercase by default
        Result.success(hex)
    } catch (e: Exception) {
        LOG.warn("Failed to read file as hex: $path", e)
        Result.failure(e)
    }

    fun readFileAsBase64(path: String): Result<String> = try {
        val bytes = Files.readAllBytes(Paths.get(path))
        val b64 = Base64.getEncoder().encodeToString(bytes)
        Result.success(b64)
    } catch (e: Exception) {
        LOG.warn("Failed to read file as base64: $path", e)
        Result.failure(e)
    }

    private fun executeActonCommand(project: Project, command: ActonCommand): Result<String> {
        val workingDir = project.guessProjectDir()?.toNioPath() ?: Paths.get("")
        val actonCommandLine = ActonCommandLine(
            command = command.name,
            workingDirectory = workingDir,
            additionalArguments = command.getArguments()
        ).toGeneralCommandLine(project) ?: return Result.failure(IllegalStateException("Cannot find acton executable"))
        return executeCommand(actonCommandLine)
    }

    private fun executeCommand(cmd: GeneralCommandLine): Result<String> {
        val processOutput = StringBuilder()

        val handler = try {
            OSProcessHandler(cmd)
        } catch (e: ProcessNotCreatedException) {
            return Result.failure(e)
        }

        handler.addProcessListener(object : CapturingProcessAdapter() {
            override fun processTerminated(event: ProcessEvent) {
                if (event.exitCode != 0) {
                    LOG.warn("Failed to execute command `${cmd.commandLineString}`: " + output.stderr)
                } else {
                    processOutput.append(output.stdout)
                }
            }
        })

        try {
            handler.startNotify()
            val future = ApplicationManager.getApplication().executeOnPooledThread {
                handler.waitFor()
            }
            future.get(10, TimeUnit.SECONDS)
        } catch (e: Exception) {
            return Result.failure(e)
        } finally {
            handler.destroyProcess()
        }

        return Result.success(processOutput.toString())
    }
}
