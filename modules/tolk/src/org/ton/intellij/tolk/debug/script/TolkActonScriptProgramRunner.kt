package org.ton.intellij.tolk.debug.script

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
import org.ton.intellij.tolk.debug.TolkDebugFailureRunContent
import org.ton.intellij.tolk.debug.TolkDapSessionStarter
import org.ton.intellij.tolk.debug.TolkRetraceDebugAdapter
import java.net.ServerSocket

class TolkActonScriptProgramRunner : AsyncProgramRunner<RunnerSettings>() {
    override fun getRunnerId(): String = RUNNER_ID

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return executorId == DefaultDebugExecutor.EXECUTOR_ID &&
            profile is ActonCommandConfiguration &&
            profile.command == "script" &&
            !profile.isScriptBroadcast()
    }

    override fun execute(
        environment: ExecutionEnvironment,
        state: RunProfileState
    ): Promise<RunContentDescriptor?> {
        val profile = environment.runProfile as? ActonCommandConfiguration
            ?: throw IllegalArgumentException("TolkActonScriptProgramRunner can only execute ActonCommandConfiguration")
        val scriptState = state as? ActonCommandRunState
            ?: throw IllegalArgumentException("TolkActonScriptProgramRunner can only execute ActonCommandRunState")
        val promise = AsyncPromise<RunContentDescriptor?>()

        ApplicationManager.getApplication().executeOnPooledThread {
            var preparedExecution: PreparedActonDebugExecution? = null
            try {
                val port = findFreePort()
                scriptState.enableScriptDebug(port)
                LOG.info("Starting acton script debug session via TolkActonScriptProgramRunner on port $port for '${profile.name}'")
                preparedExecution = scriptState.prepareForDebugLaunch(environment.executor, this)
                LOG.info(
                    "Waiting for acton script DAP readiness on port ${preparedExecution.debugSession.port} before opening XDebugger session for '${profile.name}'"
                )
                runBlocking {
                    preparedExecution.debugSession.awaitReady()
                }
                LOG.info(
                    "Acton script DAP became ready on port ${preparedExecution.debugSession.port} before session start for '${profile.name}'"
                )

                val descriptor = arrayOfNulls<RunContentDescriptor>(1)
                ApplicationManager.getApplication().invokeAndWait {
                    descriptor[0] = TolkDapSessionStarter.start(
                        environment = environment,
                        state = scriptState,
                        sessionName = profile.name,
                        adapterId = TolkRetraceDebugAdapter,
                        request = DapStartRequest.Launch,
                        arguments = emptyMap(),
                        logLabel = "acton script"
                    )
                }
                promise.setResult(descriptor[0])
            } catch (t: Throwable) {
                val fallbackDescriptor = preparedExecution?.executionResult?.let { executionResult ->
                    val descriptor = arrayOfNulls<RunContentDescriptor>(1)
                    ApplicationManager.getApplication().invokeAndWait {
                        descriptor[0] = TolkDebugFailureRunContent.showIfTerminatedBeforeDap(
                            environment = environment,
                            executionResult = executionResult,
                            error = t,
                            title = profile.name
                        )
                    }
                    descriptor[0]
                }
                if (fallbackDescriptor != null) {
                    promise.setResult(fallbackDescriptor)
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
        private const val RUNNER_ID = "TolkActonScriptProgramRunner"
        private val LOG = logger<TolkActonScriptProgramRunner>()
    }
}
