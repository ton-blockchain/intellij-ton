package org.ton.intellij.toncli.runconfig

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.util.io.BaseOutputReader

class TonCapturingProcessHandler private constructor(
        commandLine: GeneralCommandLine
) : CapturingProcessHandler(commandLine) {
    override fun readerOptions(): BaseOutputReader.Options = BaseOutputReader.Options.BLOCKING

    companion object {
        fun startProcess(commandLine: GeneralCommandLine): Result<TonCapturingProcessHandler> {
            return try {
                Result.success(TonCapturingProcessHandler(commandLine))
            } catch (e: ExecutionException) {
                Result.failure(e)
            }
        }
    }
}