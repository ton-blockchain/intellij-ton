package org.ton.intellij.tolk.debug

import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import kotlin.math.min

internal object ActonDebugProcessTermination {
    private const val PROCESS_EXIT_GRACE_MS = 2_000L
    private const val PROCESS_EXIT_POLL_MS = 50L

    fun stopWhenSocketCloses(processHandler: ProcessHandler, sessionId: String, log: Logger) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val exitedNaturally = waitForNaturalExit(
                graceMillis = PROCESS_EXIT_GRACE_MS,
                pollMillis = PROCESS_EXIT_POLL_MS,
                isAlive = { !processHandler.isProcessTerminating && !processHandler.isProcessTerminated },
            )
            if (exitedNaturally) {
                log.info("Acton debug process for session $sessionId exited naturally after DAP disconnect")
                return@executeOnPooledThread
            }

            log.info(
                "Acton debug process for session $sessionId is still alive after " +
                    "${PROCESS_EXIT_GRACE_MS}ms; stopping it",
            )
            if (!processHandler.isProcessTerminating && !processHandler.isProcessTerminated) {
                processHandler.destroyProcess()
            }
        }
    }

    internal fun waitForNaturalExit(
        graceMillis: Long,
        pollMillis: Long,
        isAlive: () -> Boolean,
        sleep: (Long) -> Unit = Thread::sleep,
        nanoTime: () -> Long = System::nanoTime,
    ): Boolean {
        if (!isAlive()) return true

        val deadlineNanos = nanoTime() + graceMillis * 1_000_000
        while (true) {
            val remainingMillis = ((deadlineNanos - nanoTime()) / 1_000_000).coerceAtLeast(0)
            if (remainingMillis == 0L) {
                return !isAlive()
            }

            try {
                sleep(min(pollMillis, remainingMillis))
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return !isAlive()
            }

            if (!isAlive()) return true
        }
    }
}
