package org.ton.intellij.acton.runconfig

import com.intellij.execution.ExecutionException
import com.intellij.openapi.util.Key
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import java.nio.file.Path

val ACTON_DEBUG_SESSION_KEY: Key<ActonDebugSession> = Key.create("acton.debug.session")

class ActonDebugSession(val displayName: String, val port: Int, private val readinessMarkers: List<String>) {
    private val readySignal = CompletableDeferred<Unit>()
    private val transcript = StringBuilder()
    private val processOutput = StringBuilder()
    private var commandLine: String? = null

    fun append(text: String) {
        synchronized(this) {
            transcript.append(text)
            processOutput.append(text)
            if (!readySignal.isCompleted && readinessMarkers.any(processOutput::contains)) {
                readySignal.complete(Unit)
            }
        }
    }

    fun recordStartup(commandLine: String, workingDirectory: Path, note: String? = null) {
        synchronized(this) {
            this.commandLine = commandLine
        }
        appendStartup(
            buildString {
                append("Starting ")
                append(displayName)
                append(" in ")
                append(workingDirectory)
                append('\n')
                append("Command: ")
                append(commandLine)
                append('\n')
                append("Waiting for DAP on 127.0.0.1:")
                append(port)
                append('\n')
                note
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let {
                        append(it)
                        append('\n')
                    }
            },
        )
    }

    fun processTerminated(exitCode: Int) {
        if (!readySignal.isCompleted) {
            readySignal.completeExceptionally(
                ExecutionException(startupFailureMessage(exitCode)),
            )
        }
    }

    suspend fun awaitReady() {
        withTimeout(120_000) {
            readySignal.await()
        }
    }

    fun transcript(): String = synchronized(this) { transcript.toString() }

    private fun appendStartup(text: String) {
        synchronized(this) {
            transcript.append(text)
        }
    }

    internal fun startupFailureMessage(exitCode: Int): String {
        val details = cleanedFailureOutput().ifBlank { transcript() }
        return buildString {
            append(displayName)
            append(" failed before DAP startup (code=")
            append(exitCode)
            append(")\n\n")
            append(details)
        }
    }

    private fun cleanedFailureOutput(): String {
        val (rawOutput, commandLine) = synchronized(this) {
            processOutput.toString() to commandLine
        }
        if (rawOutput.isBlank()) return ""

        val normalizedLines = rawOutput
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .lineSequence()
            .map(String::trimEnd)
            .toList()

        val withoutEcho = normalizedLines
            .dropWhile { line -> line.isBlank() || line == commandLine }

        val errorIndex = withoutEcho.indexOfFirst(::isDiagnosticHeader)
        val relevantLines = if (errorIndex >= 0) withoutEcho.drop(errorIndex) else withoutEcho
        return relevantLines.joinToString("\n").trim()
    }

    private fun isDiagnosticHeader(line: String): Boolean {
        val trimmed = line.trimStart()
        return trimmed.startsWith("Error:") ||
            trimmed.startsWith("error:") ||
            trimmed.startsWith("warning:") ||
            ": error:" in trimmed ||
            ": warning:" in trimmed
    }
}
