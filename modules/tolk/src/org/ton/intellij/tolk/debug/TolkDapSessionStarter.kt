package org.ton.intellij.tolk.debug

import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.diagnostic.logger
import com.intellij.platform.dap.DapProcessStarter
import com.intellij.platform.dap.DapStartRequest
import com.intellij.platform.dap.DebugAdapterId
import com.intellij.xdebugger.XDebugProcessStarter
import com.intellij.xdebugger.XDebuggerManager

internal object TolkDapSessionStarter {
    fun start(
        environment: ExecutionEnvironment,
        state: RunProfileState,
        sessionName: String,
        adapterId: DebugAdapterId,
        request: DapStartRequest,
        arguments: Map<String, Any>,
        logLabel: String,
    ): RunContentDescriptor? {
        val starter = DapProcessStarter(
            environment,
            environment.executor,
            state,
            adapterId,
            request,
            arguments,
        )
        val debuggerManager = XDebuggerManager.getInstance(environment.project)
        startWithSessionBuilder(debuggerManager, starter, environment, sessionName, logLabel)?.let { descriptor ->
            LOG.info("Started $logLabel debug session '$sessionName' via XDebugSessionBuilder")
            return descriptor
        }

        val session = debuggerManager.startSession(environment, starter)
        val descriptor = extractRunContentDescriptor(session, sessionName, logLabel)
        if (descriptor == null) {
            LOG.info(
                "Run content descriptor is not initialized for $logLabel debug session '$sessionName'; " +
                    "returning null descriptor",
            )
        }
        return descriptor
    }

    private fun startWithSessionBuilder(
        debuggerManager: XDebuggerManager,
        starter: XDebugProcessStarter,
        environment: ExecutionEnvironment,
        sessionName: String,
        logLabel: String,
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
            .onFailure {
                LOG.warn("Failed to bind ExecutionEnvironment to XDebugSessionBuilder for '$sessionName'", it)
            }
            .getOrNull()
            ?: return null

        val startedResult = runCatching {
            builder.javaClass.getMethod("startSession").invoke(builder)
        }
            .onFailure { LOG.warn("Failed to start $logLabel session via XDebugSessionBuilder for '$sessionName'", it) }
            .getOrNull()
            ?: return null

        return runCatching {
            startedResult.javaClass.getMethod("getRunContentDescriptor").invoke(startedResult) as? RunContentDescriptor
        }
            .onFailure {
                LOG.warn("Failed to obtain RunContentDescriptor from XSessionStartedResult for '$sessionName'", it)
            }
            .getOrNull()
    }

    private fun extractRunContentDescriptor(
        session: Any,
        sessionName: String,
        logLabel: String,
    ): RunContentDescriptor? {
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
            "Run content descriptor is not initialized for $logLabel debug session '$sessionName'; " +
                "deprecated XDebugSession.getRunContentDescriptor() fallback is disabled",
        )
        return null
    }

    private val LOG = logger<TolkDapSessionStarter>()
}
