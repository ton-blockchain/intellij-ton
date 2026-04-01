package org.ton.intellij.tolk.debug

import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.platform.dap.DapBreakpointsDescription
import com.intellij.platform.dap.DebugAdapterDescriptor
import com.intellij.platform.dap.DebugAdapterSupportProvider
import com.intellij.platform.dap.connection.DebugAdapterHandle
import org.ton.intellij.acton.runconfig.ACTON_DEBUG_SESSION_KEY
import kotlinx.coroutines.TimeoutCancellationException
import java.util.concurrent.CancellationException

class TolkRetraceDebugAdapterSupportProvider : DebugAdapterSupportProvider<TolkRetraceDebugAdapter> {
    override val adapterId: TolkRetraceDebugAdapter = TolkRetraceDebugAdapter

    override fun createDebugAdapterDescriptor(project: Project): DebugAdapterDescriptor<TolkRetraceDebugAdapter> {
        return object : DebugAdapterDescriptor<TolkRetraceDebugAdapter>() {
            override val id: TolkRetraceDebugAdapter = TolkRetraceDebugAdapter

            override val breakpointsDescription = DapBreakpointsDescription(
                TolkLineBreakpointType::class.java,
                TolkExceptionBreakpointType::class.java
            )

            override suspend fun launchDebugAdapter(
                environment: ExecutionEnvironment,
                executionResult: ExecutionResult?,
                sessionId: String,
            ): DebugAdapterHandle {
                val processHandler = executionResult?.processHandler
                    ?: throw ExecutionException("Acton debug process handler is not available")
                val debugSession = processHandler.getUserData(ACTON_DEBUG_SESSION_KEY)
                    ?: throw ExecutionException("Acton debug session metadata is not available")
                LOG.info("Waiting for ${debugSession.displayName} DAP readiness on port ${debugSession.port} for session $sessionId")

                try {
                    debugSession.awaitReady()
                    LOG.info("${debugSession.displayName} DAP reported ready on port ${debugSession.port} for session $sessionId")
                } catch (e: TimeoutCancellationException) {
                    stopProcess(processHandler)
                    throw ExecutionException(
                        "Timed out waiting for ${debugSession.displayName} to listen on port ${debugSession.port}\n\n${debugSession.transcript()}",
                        e
                    )
                } catch (e: CancellationException) {
                    stopProcess(processHandler)
                    throw e
                } catch (e: ExecutionException) {
                    stopProcess(processHandler)
                    throw e
                } catch (e: Throwable) {
                    stopProcess(processHandler)
                    throw ExecutionException("Failed to start Acton DAP session", e)
                }

                try {
                    LOG.info("Opening Acton DAP socket on 127.0.0.1:${debugSession.port} for session $sessionId")
                    return connectTolkRetraceDebugAdapterSocket(
                        host = "127.0.0.1",
                        port = debugSession.port
                    ) {
                        LOG.info("Stopping Acton debug process for session $sessionId")
                        stopProcess(processHandler)
                    }
                } catch (e: ExecutionException) {
                    stopProcess(processHandler)
                    throw ExecutionException(
                        "Failed to connect to ${debugSession.displayName} DAP on port ${debugSession.port}\n\n${debugSession.transcript()}",
                        e
                    )
                }
            }
        }
    }

    private fun stopProcess(processHandler: com.intellij.execution.process.ProcessHandler) {
        if (!processHandler.isProcessTerminating && !processHandler.isProcessTerminated) {
            processHandler.destroyProcess()
        }
    }

    companion object {
        private val LOG = logger<TolkRetraceDebugAdapterSupportProvider>()
    }
}
