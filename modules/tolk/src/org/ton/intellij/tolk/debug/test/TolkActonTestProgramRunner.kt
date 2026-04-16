package org.ton.intellij.tolk.debug.test

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.AsyncProgramRunner
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.platform.dap.DapStartRequest
import kotlinx.coroutines.runBlocking
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.ton.intellij.acton.runconfig.ActonCommandConfiguration
import org.ton.intellij.acton.runconfig.ActonCommandRunState
import org.ton.intellij.acton.runconfig.PreparedActonDebugExecution
import org.ton.intellij.tolk.debug.TolkDapSessionStarter
import org.ton.intellij.tolk.debug.TolkDebugFailureRunContent
import org.ton.intellij.tolk.debug.TolkRetraceDebugAdapter
import java.net.ServerSocket

class TolkActonTestProgramRunner : AsyncProgramRunner<RunnerSettings>() {
    override fun getRunnerId(): String = RUNNER_ID

    override fun canRun(executorId: String, profile: RunProfile): Boolean =
        executorId == DefaultDebugExecutor.EXECUTOR_ID &&
            profile is ActonCommandConfiguration &&
            profile.command == "test"

    override fun execute(environment: ExecutionEnvironment, state: RunProfileState): Promise<RunContentDescriptor?> {
        val profile = environment.runProfile as? ActonCommandConfiguration
            ?: throw IllegalArgumentException("TolkActonTestProgramRunner can only execute ActonCommandConfiguration")
        val testState = state as? ActonCommandRunState
            ?: throw IllegalArgumentException("TolkActonTestProgramRunner can only execute ActonCommandRunState")
        val promise = AsyncPromise<RunContentDescriptor?>()

        ApplicationManager.getApplication().executeOnPooledThread {
            var preparedExecution: PreparedActonDebugExecution? = null
            try {
                val port = findFreePort()
                testState.enableTestDebug(port)
                LOG.info(
                    "Starting acton test debug session via TolkActonTestProgramRunner on port $port for '${profile.name}'",
                )
                val preparedExecutionHolder = arrayOfNulls<PreparedActonDebugExecution>(1)
                ApplicationManager.getApplication().invokeAndWait {
                    preparedExecutionHolder[0] = testState.prepareForDebugLaunch(environment.executor, this)
                }
                preparedExecution = checkNotNull(preparedExecutionHolder[0]) {
                    "Acton test debug preparation did not return an execution result"
                }
                LOG.info(
                    "Waiting for acton test DAP readiness on port ${preparedExecution.debugSession.port} before opening XDebugger session for '${profile.name}'",
                )
                runBlocking {
                    preparedExecution.debugSession.awaitReady()
                }
                LOG.info(
                    "Acton test DAP became ready on port ${preparedExecution.debugSession.port} before session start for '${profile.name}'",
                )
                val descriptor = arrayOfNulls<RunContentDescriptor>(1)
                ApplicationManager.getApplication().invokeAndWait {
                    descriptor[0] = TolkDapSessionStarter.start(
                        environment = environment,
                        state = testState,
                        sessionName = profile.name,
                        adapterId = TolkRetraceDebugAdapter,
                        request = DapStartRequest.Launch,
                        arguments = emptyMap(),
                        logLabel = "acton test",
                    )
                }
                promise.setResult(descriptor[0])
            } catch (t: Throwable) {
                val failureShown = preparedExecution?.executionResult?.let { executionResult ->
                    val handled = booleanArrayOf(false)
                    ApplicationManager.getApplication().invokeAndWait {
                        handled[0] = TolkDebugFailureRunContent.showIfTerminatedBeforeDap(
                            environment = environment,
                            executionResult = executionResult,
                            error = t,
                            title = profile.name,
                        )
                    }
                    handled[0]
                } == true
                if (failureShown) {
                    promise.setResult(null)
                    return@executeOnPooledThread
                }
                preparedExecution?.processHandler
                    ?.takeUnless { it.isProcessTerminating || it.isProcessTerminated }
                    ?.destroyProcess()
                promise.setError(t)
            }
        }
        return promise
    }

    private fun findFreePort(): Int {
        ServerSocket(0).use { socket ->
            socket.reuseAddress = true
            return socket.localPort
        }
    }

    companion object {
        private const val RUNNER_ID = "TolkActonTestProgramRunner"
        private val LOG = logger<TolkActonTestProgramRunner>()
    }
}
