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
import kotlinx.coroutines.runBlocking
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.ton.intellij.tolk.debug.TolkDapSessionStarter

class TolkRetraceProgramRunner : AsyncProgramRunner<RunnerSettings>() {
    override fun getRunnerId(): String = RUNNER_ID

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return executorId == DefaultDebugExecutor.EXECUTOR_ID &&
            profile is TolkRetraceConfiguration
    }

    override fun execute(
        environment: ExecutionEnvironment,
        state: RunProfileState
    ): Promise<RunContentDescriptor?> {
        val profile = environment.runProfile as? TolkRetraceConfiguration
            ?: throw IllegalArgumentException("TolkRetraceProgramRunner can only execute TolkRetraceConfiguration")
        val retraceState = state as? TolkRetraceRunState
            ?: throw IllegalArgumentException("TolkRetraceProgramRunner can only execute TolkRetraceRunState")
        val promise = AsyncPromise<RunContentDescriptor?>()

        LOG.info("Starting retrace debug session via local TolkRetraceProgramRunner for '${profile.name}'")
        ApplicationManager.getApplication().executeOnPooledThread {
            var preparedExecution: PreparedRetraceExecution? = null
            try {
                LOG.info("Pre-starting retrace process for '${profile.name}' before opening XDebugger session")
                preparedExecution = retraceState.prepareForDebugLaunch(environment.executor, this)
                LOG.info(
                    "Waiting for retrace DAP readiness on port ${preparedExecution.retraceSession.port} before opening XDebugger session for '${profile.name}'"
                )
                runBlocking {
                    preparedExecution.retraceSession.awaitReady()
                }
                LOG.info(
                    "Retrace DAP became ready on port ${preparedExecution.retraceSession.port} before session start for '${profile.name}'"
                )

                val descriptor = arrayOfNulls<RunContentDescriptor>(1)
                ApplicationManager.getApplication().invokeAndWait {
                    descriptor[0] = TolkDapSessionStarter.start(
                        environment = environment,
                        state = retraceState,
                        sessionName = profile.name,
                        adapterId = TolkRetraceConfiguration.ADAPTER_ID,
                        request = TolkRetraceConfiguration.REQUEST,
                        arguments = TolkRetraceConfiguration.arguments(),
                        logLabel = "retrace"
                    )
                }
                promise.setResult(descriptor[0])
            } catch (t: Throwable) {
                preparedExecution?.processHandler
                    ?.takeUnless { it.isProcessTerminating || it.isProcessTerminated }
                    ?.destroyProcess()
                promise.setError(t)
            }
        }
        return promise
    }

    companion object {
        private const val RUNNER_ID = "TolkRetraceProgramRunner"
        private val LOG = logger<TolkRetraceProgramRunner>()
    }
}
