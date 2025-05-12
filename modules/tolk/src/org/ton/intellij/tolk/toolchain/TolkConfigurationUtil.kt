package org.ton.intellij.tolk.toolchain

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessAdapter
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessNotCreatedException
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import org.ton.intellij.tolk.TolkBundle
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.io.path.exists

object TolkConfigurationUtil {
    private val LOG = logger<TolkConfigurationUtil>()

    const val STANDARD_JS_STDLIB_PATH = "dist/tolk-stdlib"
    const val STANDARD_JS_COMPILER = "./dist/cli.js"
    const val COMPILER_REPO_STANDARD_LIB_PATH = "crypto/smartcont/tolk-stdlib"
    private val COMPILER_VERSION_REGEX = Regex("""v(\d+(?:\.\d+){1,2}(?:-[\w.]+)?(?:\+[\w.\d]+)?)""")
    val UNDEFINED_VERSION = TolkBundle["settings.tolk.toolchain.version.unknown"]

    fun guessToolchainVersion(path: String): String {
        if (path.isBlank()) {
            return UNDEFINED_VERSION
        }
        val jsCompilerPath = Path.of(path, STANDARD_JS_COMPILER)
        if (jsCompilerPath.exists()) {
            val execPath = jsCompilerPath.toAbsolutePath().toString()
            val parameters = arrayOf("--version")
            executeCommand(execPath, *parameters).onSuccess { output ->
                return COMPILER_VERSION_REGEX.find(output)?.groupValues?.get(1) ?: UNDEFINED_VERSION
            }.onFailure { e->
                LOG.warn("Couldn't get Tolk toolchain version `$execPath ${parameters.joinToString(" ")}`", e)
            }
        }

        return UNDEFINED_VERSION
    }

    private fun executeCommand(
        exePath: String,
        vararg parameters: String,
    ): Result<String> {
        val cmd = GeneralCommandLine()
            .withExePath(exePath)
            .withParameters(*parameters)
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
            future.get(1000, TimeUnit.MILLISECONDS)
        } catch (e: ExecutionException) {
            return Result.failure(e)
        } catch (e: TimeoutException) {
            return Result.failure(e)
        } finally {
            handler.destroyProcess()
        }
        return Result.success(processOutput.toString())
    }
}
