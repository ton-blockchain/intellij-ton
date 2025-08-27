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