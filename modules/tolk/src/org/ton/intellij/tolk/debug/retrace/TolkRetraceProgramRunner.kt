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
import com.intellij.platform.dap.DapProcessStarter
import com.intellij.xdebugger.XDebugProcessStarter
import com.intellij.xdebugger.XDebuggerManager
import kotlinx.coroutines.runBlocking
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise

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
                    descriptor[0] = startDebugSession(environment, retraceState, profile.name)
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

    private fun startDebugSession(
        environment: ExecutionEnvironment,
        state: TolkRetraceRunState,
        sessionName: String
    ): RunContentDescriptor? {
        val starter = DapProcessStarter(
            environment,
            environment.executor,
            state,
            TolkRetraceConfiguration.ADAPTER_ID,
            TolkRetraceConfiguration.REQUEST,
            TolkRetraceConfiguration.arguments()
        )
        val debuggerManager = XDebuggerManager.getInstance(environment.project)
        startWithSessionBuilder(debuggerManager, starter, environment, sessionName)?.let { descriptor ->
            LOG.info("Started retrace debug session '$sessionName' via XDebugSessionBuilder")
            return descriptor
        }

        val session = debuggerManager.startSessionAndShowTab(sessionName, starter, environment)
        val descriptor = extractRunContentDescriptor(session, sessionName)
        if (descriptor == null) {
            LOG.info(
                "Run content descriptor is not initialized for retrace debug session '$sessionName'; " +
                    "returning null descriptor for split debugger compatibility"
            )
        }
        return descriptor
    }

    private fun startWithSessionBuilder(
        debuggerManager: XDebuggerManager,
        starter: XDebugProcessStarter,
        environment: ExecutionEnvironment,
        sessionName: String
    ): RunContentDescriptor? {
        val builder = runCatching {
            debuggerManager.javaClass
                .getMethod("newSessionBuilder", XDebugProcessStarter::class.java)
                .invoke(debuggerManager, starter)
        }
            .onFailure { LOG.info("XDebugSessionBuilder API is not available for '$sessionName'", it) }
            .getOrNull()
            ?: return null

        runCatching {
            builder.javaClass
                .getMethod("environment", ExecutionEnvironment::class.java)
                .invoke(builder, environment)
        }
            .onFailure { LOG.warn("Failed to bind ExecutionEnvironment to XDebugSessionBuilder for '$sessionName'", it) }
            .getOrNull()
            ?: return null

        val startedResult = runCatching {
            builder.javaClass.getMethod("startSession").invoke(builder)
        }
            .onFailure { LOG.warn("Failed to start retrace session via XDebugSessionBuilder for '$sessionName'", it) }
            .getOrNull()
            ?: return null

        return runCatching {
            startedResult.javaClass.getMethod("getRunContentDescriptor").invoke(startedResult) as? RunContentDescriptor
        }
            .onFailure { LOG.warn("Failed to obtain RunContentDescriptor from XSessionStartedResult for '$sessionName'", it) }
            .getOrNull()
    }

    private fun extractRunContentDescriptor(session: Any, sessionName: String): RunContentDescriptor? {
        session.javaClass.methods
            .firstOrNull { it.name == "getRunContentDescriptorIfInitialized" && it.parameterCount == 0 }
            ?.let { method ->
                runCatching { method.invoke(session) as? RunContentDescriptor }
                    .onFailure { LOG.warn("Failed to invoke getRunContentDescriptorIfInitialized reflectively", it) }
                    .getOrNull()
                    ?.let { return it }
            }

        generateSequence(session.javaClass) { it.superclass }
            .mapNotNull { clazz ->
                runCatching { clazz.getDeclaredField("myRunContentDescriptor") }.getOrNull()
            }
            .firstOrNull()
            ?.let { field ->
                runCatching {
                    field.isAccessible = true
                    field.get(session) as? RunContentDescriptor
                }
                    .onFailure { LOG.warn("Failed to read myRunContentDescriptor reflectively", it) }
                    .getOrNull()
                    ?.let { return it }
            }

        LOG.warn(
            "Run content descriptor is not initialized for retrace debug session '$sessionName'; " +
                "deprecated XDebugSession.getRunContentDescriptor() fallback is disabled"
        )
        return null
    }

    companion object {
        private const val RUNNER_ID = "TolkRetraceProgramRunner"
        private val LOG = logger<TolkRetraceProgramRunner>()
    }
}
