package org.ton.intellij.boc

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessAdapter
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessNotCreatedException
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Base64
import java.util.HexFormat

object TasmService {
    private val LOG = logger<TasmService>()

    fun isAvailable(): Boolean {
        return try {
            val result = executeCommand("tdisasm", "--help")
            result.isSuccess
        } catch (e: Exception) {
            false
        }
    }

    fun disassemble(bocFilePath: String) = executeCommand("tdisasm", bocFilePath)
    fun showCellTree(bocFilePath: String) = executeCommand("tdisasm", "--cell-tree", bocFilePath)

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

    private fun executeCommand(vararg parameters: String): Result<String> {
        val cmd = GeneralCommandLine()
            .withExePath(parameters.first())
            .withParameters(*parameters.drop(1).toTypedArray())
            .withCharset(StandardCharsets.UTF_8)

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
