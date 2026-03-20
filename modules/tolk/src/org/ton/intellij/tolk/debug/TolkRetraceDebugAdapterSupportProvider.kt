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
import kotlinx.coroutines.TimeoutCancellationException
import org.ton.intellij.tolk.debug.retrace.TolkRetraceRunState
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
                sessionId: String
            ): DebugAdapterHandle {
                val processHandler = executionResult?.processHandler
                    ?: throw ExecutionException("Retrace process handler is not available")
                val retraceSession = processHandler.getUserData(TolkRetraceRunState.RETRACE_SESSION_KEY)
                    ?: throw ExecutionException("Retrace session metadata is not available")
                LOG.info("Waiting for retrace DAP readiness on port ${retraceSession.port} for session $sessionId")

                try {
                    retraceSession.awaitReady()
                    LOG.info("Retrace DAP reported ready on port ${retraceSession.port} for session $sessionId")
                } catch (e: TimeoutCancellationException) {
                    stopProcess(processHandler)
                    throw ExecutionException(
                        "Timed out waiting for acton retrace to listen on port ${retraceSession.port}\n\n${retraceSession.transcript()}",
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
                    throw ExecutionException("Failed to start retrace DAP session", e)
                }

                try {
                    LOG.info("Opening retrace DAP socket on 127.0.0.1:${retraceSession.port} for session $sessionId")
                    return connectTolkRetraceDebugAdapterSocket("127.0.0.1", retraceSession.port) {
                        LOG.info("Stopping retrace process for session $sessionId")
                        stopProcess(processHandler)
                    }
                } catch (e: ExecutionException) {
                    stopProcess(processHandler)
                    throw ExecutionException(
                        "Failed to connect to retrace DAP on port ${retraceSession.port}\n\n${retraceSession.transcript()}",
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
