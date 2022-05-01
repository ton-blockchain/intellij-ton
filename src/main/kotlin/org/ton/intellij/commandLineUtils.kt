package org.ton.intellij

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ElevationService
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.io.systemIndependentPath
import org.ton.intellij.toncli.runconfig.TonCapturingProcessHandler
import java.nio.file.Path

private val LOG: Logger = Logger.getInstance("org.ton.intellij.CommandLineUtils")

@Suppress("FunctionName", "UnstableApiUsage")
fun GeneralCommandLine(path: Path, withSudo: Boolean = false, vararg args: String) =
        object : GeneralCommandLine(path.systemIndependentPath, *args) {
            override fun createProcess(): Process = if (withSudo) {
                ElevationService.getInstance().createProcess(this)
            } else {
                super.createProcess()
            }
        }

fun GeneralCommandLine.withWorkDirectory(path: Path?) = withWorkDirectory(path?.systemIndependentPath)

fun GeneralCommandLine.execute(timeoutInMilliseconds: Int?): ProcessOutput? {
    LOG.info("Executing `$commandLineString`")
    val handler = TonCapturingProcessHandler.startProcess(this).getOrElse {
        LOG.warn("Failed to run executable", it)
        return null
    }
    val output = handler.runProcessWithGlobalProgress(timeoutInMilliseconds)

    if (!output.isSuccess) {
        LOG.warn(TonProcessExecutionException.errorMessage(commandLineString, output))
    }

    return output
}

private fun CapturingProcessHandler.runProcessWithGlobalProgress(timeoutInMilliseconds: Int? = null): ProcessOutput {
    return runProcess(ProgressManager.getGlobalProgressIndicator(), timeoutInMilliseconds)
}

fun CapturingProcessHandler.runProcess(
        indicator: ProgressIndicator?,
        timeoutInMilliseconds: Int? = null
): ProcessOutput {
    return when {
        indicator != null && timeoutInMilliseconds != null ->
            runProcessWithProgressIndicator(indicator, timeoutInMilliseconds)

        indicator != null -> runProcessWithProgressIndicator(indicator)
        timeoutInMilliseconds != null -> runProcess(timeoutInMilliseconds)
        else -> runProcess()
    }
}

val ProcessOutput.isSuccess: Boolean get() = !isTimeout && !isCancelled && exitCode == 0
