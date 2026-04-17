@file:Suppress("UnstableApiUsage")

package org.ton.intellij.tolk.debug

import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.platform.dap.DapBreakpointsDescription
import com.intellij.platform.dap.DapDebugSession
import com.intellij.platform.dap.DapStartRequest
import com.intellij.platform.dap.DebugAdapterDescriptor
import com.intellij.platform.dap.DebugAdapterSupportProvider
import com.intellij.platform.dap.connection.DebugAdapterHandle
import com.intellij.platform.dap.connection.DebugAdapterSocketConnection
import com.intellij.platform.dap.xdebugger.DapXDebugProcess
import com.intellij.xdebugger.XDebugSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import org.ton.intellij.acton.runconfig.ACTON_DEBUG_SESSION_KEY
import java.util.concurrent.CancellationException
import kotlin.time.Duration.Companion.milliseconds

class TolkRetraceDebugAdapterSupportProvider : DebugAdapterSupportProvider<TolkRetraceDebugAdapter> {
    override val adapterId: TolkRetraceDebugAdapter = TolkRetraceDebugAdapter

    override fun createDebugAdapterDescriptor(project: Project): DebugAdapterDescriptor<TolkRetraceDebugAdapter> {
        return object : DebugAdapterDescriptor<TolkRetraceDebugAdapter>() {
            override val id: TolkRetraceDebugAdapter = TolkRetraceDebugAdapter

            override val breakpointsDescription = DapBreakpointsDescription(
                TolkLineBreakpointType::class.java,
                TolkExceptionBreakpointType::class.java,
            )

            override fun createXDebugProcess(
                session: XDebugSession,
                dapDebugSession: DapDebugSession,
                xDebugProcessScope: CoroutineScope,
                globalScope: CoroutineScope,
                debugAdapterDescriptor: DebugAdapterDescriptor<*>,
                executionEnvironment: ExecutionEnvironment,
                executionResult: ExecutionResult?,
                startRequestType: DapStartRequest,
                startRequestArguments: Map<String, Any?>,
            ): DapXDebugProcess {
                val preparedExecutionResult = executionResult
                    ?: return super.createXDebugProcess(
                        session,
                        dapDebugSession,
                        xDebugProcessScope,
                        globalScope,
                        debugAdapterDescriptor,
                        executionEnvironment,
                        executionResult,
                        startRequestType,
                        startRequestArguments,
                    )
                return TolkDapXDebugProcess(
                    session = session,
                    dapDebugSession = dapDebugSession,
                    xDebugProcessScope = xDebugProcessScope,
                    globalScope = globalScope,
                    debugAdapterDescriptor = debugAdapterDescriptor,
                    executionEnvironment = executionEnvironment,
                    backingExecutionResult = preparedExecutionResult,
                    startRequestType = startRequestType,
                    startRequestArguments = startRequestArguments,
                )
            }

            override suspend fun launchDebugAdapter(
                environment: ExecutionEnvironment,
                executionResult: ExecutionResult?,
                sessionId: String,
            ): DebugAdapterHandle {
                val processHandler = executionResult?.processHandler
                    ?: throw ExecutionException("Acton debug process handler is not available")
                val debugSession = processHandler.getUserData(ACTON_DEBUG_SESSION_KEY)
                    ?: throw ExecutionException("Acton debug session metadata is not available")
                LOG.info(
                    "Waiting for ${debugSession.displayName} DAP readiness on port ${debugSession.port} for session $sessionId",
                )

                try {
                    debugSession.awaitReady()
                    LOG.info(
                        "${debugSession.displayName} DAP reported ready on port ${debugSession.port} for session $sessionId",
                    )
                } catch (e: TimeoutCancellationException) {
                    stopProcess(processHandler)
                    throw ExecutionException(
                        "Timed out waiting for ${debugSession.displayName} to listen on port ${debugSession.port}\n\n${debugSession.transcript()}",
                        e,
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
                    return DebugAdapterSocketConnection(
                        host = "127.0.0.1",
                        port = debugSession.port,
                        connectionAttempts = 3,
                        intervalBetweenAttempts = 300.milliseconds,
                    ) {
                        LOG.info("DAP socket closed for session $sessionId")
                        ActonDebugProcessTermination.stopWhenSocketCloses(processHandler, sessionId, LOG)
                    }
                } catch (e: CancellationException) {
                    stopProcess(processHandler)
                    throw e
                } catch (e: Throwable) {
                    stopProcess(processHandler)
                    throw ExecutionException(
                        "Failed to connect to ${debugSession.displayName} DAP on port ${debugSession.port}\n\n${debugSession.transcript()}",
                        e,
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
