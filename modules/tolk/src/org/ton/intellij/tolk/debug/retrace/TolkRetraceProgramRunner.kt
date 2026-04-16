package org.ton.intellij.tolk.debug.retrace

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
import org.ton.intellij.acton.cli.ActonToml
import org.ton.intellij.acton.runconfig.ActonCommandConfiguration
import org.ton.intellij.acton.runconfig.ActonCommandRunState
import org.ton.intellij.acton.runconfig.PreparedActonDebugExecution
import org.ton.intellij.tolk.debug.TolkDapSessionStarter
import org.ton.intellij.tolk.debug.TolkDebugFailureRunContent
import org.ton.intellij.tolk.debug.TolkRetraceDebugAdapter
import java.net.ServerSocket

class TolkRetraceProgramRunner : AsyncProgramRunner<RunnerSettings>() {
    override fun getRunnerId(): String = RUNNER_ID

    override fun canRun(executorId: String, profile: RunProfile): Boolean =
        executorId == DefaultDebugExecutor.EXECUTOR_ID &&
            profile is ActonCommandConfiguration &&
            profile.command == "retrace"

    override fun execute(environment: ExecutionEnvironment, state: RunProfileState): Promise<RunContentDescriptor?> {
        val profile = environment.runProfile as? ActonCommandConfiguration
            ?: throw IllegalArgumentException("TolkRetraceProgramRunner can only execute ActonCommandConfiguration")
        val retraceState = state as? ActonCommandRunState
            ?: throw IllegalArgumentException("TolkRetraceProgramRunner can only execute ActonCommandRunState")
        val promise = AsyncPromise<RunContentDescriptor?>()

        ApplicationManager.getApplication().executeOnPooledThread {
            var preparedExecution: PreparedActonDebugExecution? = null
            try {
                val contractId = resolveContractId(profile) ?: run {
                    promise.setResult(null)
                    return@executeOnPooledThread
                }
                val port = findFreePort()
                retraceState.enableRetraceDebug(port, contractId)
                LOG.info(
                    "Starting acton retrace debug session via TolkRetraceProgramRunner on port $port for '${profile.name}'",
                )
                preparedExecution = retraceState.prepareForDebugLaunch(environment.executor, this)
                LOG.info(
                    "Waiting for acton retrace DAP readiness on port ${preparedExecution.debugSession.port} before opening XDebugger session for '${profile.name}'",
                )
                runBlocking {
                    preparedExecution.debugSession.awaitReady()
                }
                LOG.info(
                    "Acton retrace DAP became ready on port ${preparedExecution.debugSession.port} before session start for '${profile.name}'",
                )

                val descriptor = arrayOfNulls<RunContentDescriptor>(1)
                ApplicationManager.getApplication().invokeAndWait {
                    descriptor[0] = TolkDapSessionStarter.start(
                        environment = environment,
                        state = retraceState,
                        sessionName = profile.name,
                        adapterId = TolkRetraceDebugAdapter,
                        request = DapStartRequest.Launch,
                        arguments = emptyMap(),
                        logLabel = "acton retrace",
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

    private fun resolveContractId(profile: ActonCommandConfiguration): String? {
        profile.retraceContractId.trim().takeIf { it.isNotBlank() }?.let { return it }

        val resolved = arrayOfNulls<String>(1)
        ApplicationManager.getApplication().invokeAndWait {
            resolved[0] = TolkRetraceLauncher.chooseContractId(profile.project, ActonToml.find(profile.project))
        }
        return resolved[0]
    }

    private fun findFreePort(): Int {
        ServerSocket(0).use { socket ->
            socket.reuseAddress = true
            return socket.localPort
        }
    }

    companion object {
        private const val RUNNER_ID = "TolkRetraceProgramRunner"
        private val LOG = logger<TolkRetraceProgramRunner>()
    }
}
