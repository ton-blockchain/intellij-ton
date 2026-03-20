package org.ton.intellij.tolk.debug.retrace

import com.intellij.execution.ExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.diagnostic.logger
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.util.Key
import org.ton.intellij.acton.cli.ActonCommandLine
import org.ton.intellij.acton.runconfig.ACTON_DEBUG_SESSION_KEY
import org.ton.intellij.acton.runconfig.ActonDebugSession
import java.net.ServerSocket

class TolkRetraceRunState(
    environment: ExecutionEnvironment,
    private val configuration: TolkRetraceConfiguration
) : CommandLineState(environment) {
    private val executionLock = Any()
    @Volatile
    private var cachedExecutionResult: ExecutionResult? = null

    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
        cachedExecutionResult?.let { return it }
        synchronized(executionLock) {
            cachedExecutionResult?.let { return it }
            return super.execute(executor, runner).also { cachedExecutionResult = it }
        }
    }

    fun prepareForDebugLaunch(executor: Executor, runner: ProgramRunner<*>): PreparedRetraceExecution {
        val executionResult = execute(executor, runner)
        val processHandler = executionResult.processHandler
            ?: throw ExecutionException("Retrace process handler is not available")
        if (!processHandler.isStartNotified) {
            processHandler.startNotify()
            LOG.info("Started process notifications for retrace DAP session on port ${processHandler.getUserData(ACTON_DEBUG_SESSION_KEY)?.port}")
        }
        val retraceSession = processHandler.getUserData(ACTON_DEBUG_SESSION_KEY)
            ?: throw ExecutionException("Retrace session metadata is not available")
        return PreparedRetraceExecution(executionResult, processHandler, retraceSession)
    }

    override fun startProcess(): ProcessHandler {
        val workingDir = configuration.workingDirectory ?: throw IllegalStateException("Working directory is not set")
        val port = findFreePort()
        val arguments = buildList {
            add(configuration.transactionHash)
            if (configuration.network.isNotBlank()) {
                add("--net")
                add(configuration.network)
            }
            add("--contract")
            add(configuration.contractId)
            add("--dap-port")
            add(port.toString())
        }
        val commandLine = ActonCommandLine(
            command = "retrace",
            workingDirectory = workingDir,
            additionalArguments = arguments
        ).toGeneralCommandLine(configuration.project) ?: throw IllegalStateException("Cannot find acton executable")

        val processHandler = KillableColoredProcessHandler(commandLine)
        val retraceSession = ActonDebugSession(
            displayName = "acton retrace",
            port = port,
            readinessMarkers = listOf("Retrace DAP listening on 127.0.0.1:$port")
        )
        retraceSession.recordStartup(
            commandLine.commandLineString,
            workingDir,
            "DAP server is started only after retrace preparation completes."
        )
        LOG.info("Starting retrace DAP session on 127.0.0.1:$port with command: ${commandLine.commandLineString}")
        processHandler.putUserData(ACTON_DEBUG_SESSION_KEY, retraceSession)
        processHandler.addProcessListener(object : ProcessAdapter() {
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                retraceSession.append(event.text)
                event.text
                    .trimEnd('\r', '\n')
                    .takeIf { it.isNotBlank() }
                    ?.let { LOG.info("acton retrace [$port][$outputType] $it") }
            }

            override fun processTerminated(event: ProcessEvent) {
                LOG.info("acton retrace [$port] terminated with exit code ${event.exitCode}")
                retraceSession.processTerminated(event.exitCode)
            }
        })
        return processHandler
    }

    override fun createConsole(executor: Executor): ConsoleView {
        return TextConsoleBuilderFactory.getInstance().createBuilder(configuration.project).console
    }

    private fun findFreePort(): Int {
        ServerSocket(0).use { socket ->
            socket.reuseAddress = true
            return socket.localPort
        }
    }

    companion object {
        private val LOG = logger<TolkRetraceRunState>()
    }
}

data class PreparedRetraceExecution(
    val executionResult: ExecutionResult,
    val processHandler: ProcessHandler,
    val retraceSession: ActonDebugSession
)
