package org.ton.intellij.acton.runconfig

import com.intellij.execution.ExecutionException
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.Key
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.net.BindException
import java.net.InetAddress
import java.net.ServerSocket
import java.nio.file.Path

val ACTON_DEBUG_SESSION_KEY: Key<ActonDebugSession> = Key.create("acton.debug.session")

class ActonDebugSession(
    val displayName: String,
    val port: Int,
    private val readinessMarkers: List<String>
) {
    private val readySignal = CompletableDeferred<Unit>()
    private val transcript = StringBuilder()
    private val log = logger<ActonDebugSession>()

    fun append(text: String) {
        synchronized(this) {
            transcript.append(text)
            if (!readySignal.isCompleted && readinessMarkers.any(transcript::contains)) {
                readySignal.complete(Unit)
            }
        }
    }

    fun recordStartup(commandLine: String, workingDirectory: Path, note: String? = null) {
        append(
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
            }
        )
    }

    fun processTerminated(exitCode: Int) {
        if (!readySignal.isCompleted) {
            readySignal.completeExceptionally(
                ExecutionException("$displayName exited before DAP startup (code=$exitCode)\n\n${transcript()}")
            )
        }
    }

    suspend fun awaitReady() {
        withTimeout(120_000) {
            while (!readySignal.isCompleted) {
                if (isPortReservedByProcess()) {
                    log.info("Detected $displayName DAP listener on port $port by socket bind check")
                    readySignal.complete(Unit)
                    break
                }
                delay(200)
            }
            readySignal.await()
        }
    }

    fun transcript(): String = synchronized(this) { transcript.toString() }

    private fun isPortReservedByProcess(): Boolean {
        return try {
            ServerSocket(port, 0, InetAddress.getByName("127.0.0.1")).use { false }
        } catch (_: BindException) {
            true
        }
    }
}
