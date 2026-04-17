package org.ton.intellij.acton.runconfig

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.filters.TextConsoleBuilderImpl
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.Key
import com.intellij.psi.search.ExecutionSearchScopes
import com.intellij.terminal.TerminalExecutionConsole
import com.intellij.util.execution.ParametersListUtil
import org.ton.intellij.acton.cli.ActonCommand
import org.ton.intellij.acton.cli.ActonCommandLine
import org.ton.intellij.acton.ide.ActonBacktraceConsoleFilter
import org.ton.intellij.acton.profiler.ActonProfilerSupport

class ActonCommandRunState(environment: ExecutionEnvironment, private val configuration: ActonCommandConfiguration) :
    CommandLineState(environment) {
    private val executionLock = Any()

    @Volatile
    private var cachedExecutionResult: ExecutionResult? = null

    @Volatile
    private var scriptDebugPortOverride: Int? = null

    @Volatile
    private var testDebugPortOverride: Int? = null

    @Volatile
    private var retraceDebugPortOverride: Int? = null

    @Volatile
    private var retraceDebugContractIdOverride: String? = null

    init {
        if (configuration.emulateTerminal) {
            consoleBuilder = object : TextConsoleBuilderImpl(
                environment.project,
                ExecutionSearchScopes.executionScope(environment.project, configuration),
            ) {
                override fun createConsole(): ConsoleView = TerminalExecutionConsole(project, null)
            }
        }
    }

    fun enableScriptDebug(port: Int) {
        scriptDebugPortOverride = port
    }

    fun enableTestDebug(port: Int) {
        testDebugPortOverride = port
    }

    fun enableRetraceDebug(port: Int, contractId: String) {
        retraceDebugPortOverride = port
        retraceDebugContractIdOverride = contractId
    }

    fun prepareForDebugLaunch(executor: Executor, runner: ProgramRunner<*>): PreparedActonDebugExecution {
        val executionResult = execute(executor, runner)
        val processHandler = executionResult.processHandler
            ?: throw ExecutionException("Acton debug process handler is not available")
        if (!processHandler.isStartNotified) {
            processHandler.startNotify()
            LOG.info(
                "Started process notifications for ${processHandler.getUserData(
                    ACTON_DEBUG_SESSION_KEY,
                )?.displayName ?: "acton debug"} " +
                    "DAP session on port ${processHandler.getUserData(ACTON_DEBUG_SESSION_KEY)?.port}",
            )
        }
        val debugSession = processHandler.getUserData(ACTON_DEBUG_SESSION_KEY)
            ?: throw ExecutionException("Acton debug session metadata is not available")
        return PreparedActonDebugExecution(executionResult, processHandler, debugSession)
    }

    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
        cachedExecutionResult?.let { return it }
        synchronized(executionLock) {
            cachedExecutionResult?.let { return it }
            return doExecute(executor, runner).also { cachedExecutionResult = it }
        }
    }

    private fun doExecute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
        if (configuration.command == "test") {
            val consoleProperties = ActonTestConsolePropertiesProvider.EP_NAME.extensionList.firstNotNullOfOrNull {
                it.createConsoleProperties(configuration, executor)
            }
            if (consoleProperties != null) {
                val console = SMTestRunnerConnectionUtil.createConsole(
                    consoleProperties.testFrameworkName,
                    consoleProperties,
                ) as SMTRunnerConsoleView
                console.addMessageFilter(ActonBacktraceConsoleFilter(configuration))
                val handler = startProcess()
                console.attachToProcess(handler)
                val testFailureState = configuration.project.actonTestFailureState

                val connection = configuration.project.messageBus.connect(console)
                connection.subscribe(
                    SMTRunnerEventsListener.TEST_STATUS,
                    object : SMTRunnerEventsAdapter() {
                        override fun onTestStarted(test: SMTestProxy) {
                            testFailureState.clear(test.locationUrl)
                        }

                        override fun onTestFailed(test: SMTestProxy) {
                            testFailureState.update(test)
                        }

                        override fun onBeforeTestingFinished(testsRoot: SMTestProxy.SMRootTestProxy) {
                            testsRoot.setFinished()
                        }
                    },
                )

                val smTestProxy = console.resultsViewer.root as SMTestProxy.SMRootTestProxy
                smTestProxy.setTestsReporterAttached()

                val executionResult = DefaultExecutionResult(console, handler)
                consoleProperties.createRerunFailedTestsAction(console)?.let { rerunFailedTestsAction ->
                    rerunFailedTestsAction.setModelProvider { console.resultsViewer }
                    executionResult.setRestartActions(rerunFailedTestsAction)
                }
                return executionResult
            }
        }
        return super.execute(executor, runner)
    }

    override fun startProcess(): ProcessHandler {
        val workingDir = configuration.workingDirectory
            ?: configuration.project.guessProjectDir()?.toNioPath()
            ?: throw IllegalStateException("Working directory not set")

        val actonCommand = createActonCommand()
        val additionalArgs = ParametersListUtil.parse(configuration.parameters)

        val args = actonCommand.getArguments().toMutableList()
        if (configuration.command == "test" && environment.executor.isActonCoverageExecutor()) {
            if (!isActonCoverageSupported()) {
                throw ExecutionException(ACTON_COVERAGE_UNSUPPORTED_MESSAGE)
            }
            args.add("--coverage")
            args.add("--coverage-format")
            args.add("lcov")
        }
        val profilerSession = createProfilerSessionIfNeeded(environment.executor)
        profilerSession?.let { args.addAll(it.additionalArguments) }

        val actonCommandLine = ActonCommandLine(
            command = actonCommand.name,
            workingDirectory = workingDir,
            additionalArguments = args + additionalArgs,
            environmentVariables = configuration.env,
        )

        var commandLine =
            actonCommandLine.toGeneralCommandLine(configuration.project)
                ?: throw IllegalStateException("Cannot find acton executable")
        if (configuration.emulateTerminal) {
            commandLine = PtyCommandLine(commandLine)
                .withInitialColumns(PtyCommandLine.MAX_COLUMNS)
                .withConsoleMode(false)
        }

        val handler = KillableColoredProcessHandler(commandLine)
        profilerSession?.attachToProcess(handler)
        attachDebugSessionIfNeeded(handler, commandLine, workingDir, actonCommand)
        return handler
    }

    private fun createActonCommand() = when (val command = configuration.getActonCommand()) {
        is ActonCommand.Script -> {
            val debugPort = scriptDebugPortOverride?.toString() ?: command.debugPort
            command.copy(
                debug = scriptDebugPortOverride != null || command.debug,
                debugPort = debugPort,
            )
        }
        is ActonCommand.Test -> createTestCommand(command, environment, testDebugPortOverride)
        is ActonCommand.Retrace -> {
            val debugPort = retraceDebugPortOverride?.toString() ?: command.debugPort
            command.copy(
                contractId = retraceDebugContractIdOverride ?: command.contractId,
                debug = retraceDebugPortOverride != null || command.debug,
                debugPort = debugPort,
            )
        }
        else -> command
    }

    private fun attachDebugSessionIfNeeded(
        handler: KillableColoredProcessHandler,
        commandLine: GeneralCommandLine,
        workingDir: java.nio.file.Path,
        actonCommand: ActonCommand,
    ) {
        val debugInfo = when (actonCommand) {
            is ActonCommand.Script -> {
                if (!isDebugScript(actonCommand)) return
                DebugSessionInfo(
                    displayName = "acton script",
                    port = actonCommand.debugPort.toIntOrNull() ?: return,
                    readinessMarkers = listOf(
                        "Debugger server listening on 127.0.0.1:${actonCommand.debugPort}",
                        "Retrace DAP listening on 127.0.0.1:${actonCommand.debugPort}",
                    ),
                )
            }
            is ActonCommand.Test -> {
                if (!isDebugTest(actonCommand)) return
                DebugSessionInfo(
                    displayName = "acton test",
                    port = actonCommand.debugPort.toIntOrNull() ?: return,
                    readinessMarkers = listOf("Debugger server listening on 127.0.0.1:${actonCommand.debugPort}"),
                )
            }
            is ActonCommand.Retrace -> {
                if (!isDebugRetrace(actonCommand)) return
                DebugSessionInfo(
                    displayName = "acton retrace",
                    port = actonCommand.debugPort.toIntOrNull() ?: return,
                    readinessMarkers = listOf(
                        "Debugger listening on 127.0.0.1:${actonCommand.debugPort}",
                        "Retrace DAP listening on 127.0.0.1:${actonCommand.debugPort}",
                    ),
                )
            }
            else -> return
        }

        val debugSession = ActonDebugSession(
            displayName = debugInfo.displayName,
            port = debugInfo.port,
            readinessMarkers = debugInfo.readinessMarkers,
        )
        debugSession.recordStartup(commandLine.commandLineString, workingDir)
        handler.putUserData(ACTON_DEBUG_SESSION_KEY, debugSession)
        handler.addProcessListener(object : ProcessAdapter() {
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                debugSession.append(event.text)
                event.text
                    .trimEnd('\r', '\n')
                    .takeIf { it.isNotBlank() }
                    ?.let { LOG.info("${debugInfo.displayName} [${debugInfo.port}][$outputType] $it") }
            }

            override fun processTerminated(event: ProcessEvent) {
                LOG.info("${debugInfo.displayName} [${debugInfo.port}] terminated with exit code ${event.exitCode}")
                debugSession.processTerminated(event.exitCode)
            }
        })
    }

    private fun isDebugScript(actonCommand: ActonCommand): Boolean {
        val scriptCommand = actonCommand as? ActonCommand.Script ?: return false
        return scriptCommand.debug && scriptCommand.broadcastNet.isBlank()
    }

    private fun isDebugTest(actonCommand: ActonCommand): Boolean {
        val testCommand = actonCommand as? ActonCommand.Test ?: return false
        return testCommand.debug
    }

    private fun isDebugRetrace(actonCommand: ActonCommand): Boolean {
        val retraceCommand = actonCommand as? ActonCommand.Retrace ?: return false
        return retraceCommand.debug
    }

    private fun createProfilerSessionIfNeeded(
        executor: Executor,
    ): org.ton.intellij.acton.profiler.ActonProfilerSession? {
        if (configuration.command != "test") return null
        return ActonProfilerSupport.EP_NAME.extensionList.firstNotNullOfOrNull {
            it.createTestSession(configuration, executor)
        }
    }

    companion object {
        val TEST_COMMAND_OVERRIDE_KEY = Key.create<TestCommandOverride>("org.ton.intellij.acton.testCommandOverride")

        internal fun createTestCommand(
            command: ActonCommand.Test,
            environment: ExecutionEnvironment,
            debugPortOverride: Int?,
        ): ActonCommand.Test {
            val override = environment.getUserData(TEST_COMMAND_OVERRIDE_KEY)
            if (override != null) {
                environment.putUserData(TEST_COMMAND_OVERRIDE_KEY, null)
            }

            val overriddenCommand = if (override != null) {
                command.copy(
                    mode = override.mode,
                    target = override.target,
                    functionName = override.functionName,
                )
            } else {
                command
            }

            val debugPort = debugPortOverride?.toString() ?: overriddenCommand.debugPort
            return overriddenCommand.copy(
                debug = debugPortOverride != null || overriddenCommand.debug,
                debugPort = debugPort,
            )
        }

        private val LOG = logger<ActonCommandRunState>()
    }
}

data class TestCommandOverride(val mode: ActonCommand.Test.TestMode, val target: String, val functionName: String)

private data class DebugSessionInfo(val displayName: String, val port: Int, val readinessMarkers: List<String>)

data class PreparedActonDebugExecution(
    val executionResult: ExecutionResult,
    val processHandler: ProcessHandler,
    val debugSession: ActonDebugSession,
)
