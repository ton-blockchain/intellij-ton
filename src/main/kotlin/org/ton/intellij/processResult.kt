package org.ton.intellij

import com.intellij.execution.ExecutionException
import com.intellij.execution.process.ProcessOutput

sealed class TonProcessExecutionOrDeserializationException : RuntimeException {
    constructor(cause: Throwable) : super(cause)
    constructor(message: String) : super(message)
}


sealed class TonProcessExecutionException : TonProcessExecutionOrDeserializationException {
    constructor(message: String) : super(message)
    constructor(cause: Throwable) : super(cause)

    abstract val commandLineString: String

    class Start(
            override val commandLineString: String,
            cause: ExecutionException,
    ) : TonProcessExecutionException(cause)

    class Canceled(
            override val commandLineString: String,
            val output: ProcessOutput,
            message: String = errorMessage(commandLineString, output),
    ) : TonProcessExecutionException(message)

    class Timeout(
            override val commandLineString: String,
            val output: ProcessOutput,
    ) : TonProcessExecutionException(errorMessage(commandLineString, output))

    /** The process exited with non-zero exit code */
    class ProcessAborted(
            override val commandLineString: String,
            val output: ProcessOutput,
    ) : TonProcessExecutionException(errorMessage(commandLineString, output))

    companion object {
        fun errorMessage(commandLineString: String, output: ProcessOutput): String = """
            |Execution failed (exit code ${output.exitCode}).
            |$commandLineString
            |stdout : ${output.stdout}
            |stderr : ${output.stderr}
        """.trimMargin()
    }
}
