package org.ton.intellij.acton.runconfig

import com.intellij.coverage.CoverageExecutor
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PtyCommandLine
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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.Key
import com.intellij.profiler.actions.ImportProfilerResultAction
import com.intellij.profiler.api.ProfilerDumpDescriptor
import com.intellij.profiler.api.ProfilerDumpManager
import com.intellij.profiler.clion.ProfilerExecutor
import com.intellij.util.execution.ParametersListUtil
import org.ton.intellij.acton.cli.ActonCommand
import org.ton.intellij.acton.cli.ActonCommandLine
import org.ton.intellij.acton.profiler.ActonCollapsedProfileDumpParserProvider
import java.util.concurrent.ConcurrentHashMap

class ActonCommandRunState(
    environment: ExecutionEnvironment,
    private val configuration: ActonCommandConfiguration,
) : CommandLineState(environment) {
    private val executionLock = Any()
    @Volatile
    private var cachedExecutionResult: ExecutionResult? = null
    @Volatile
    private var scriptDebugPortOverride: Int? = null
    @Volatile
    private var testDebugPortOverride: Int? = null

    fun enableScriptDebug(port: Int) {
        scriptDebugPortOverride = port
    }

    fun enableTestDebug(port: Int) {
        testDebugPortOverride = port
    }

    fun prepareForDebugLaunch(executor: Executor, runner: ProgramRunner<*>): PreparedActonDebugExecution {
        val executionResult = execute(executor, runner)
        val processHandler = executionResult.processHandler
            ?: throw ExecutionException("Acton debug process handler is not available")
        if (!processHandler.isStartNotified) {
            processHandler.startNotify()
            LOG.info(
                "Started process notifications for ${processHandler.getUserData(ACTON_DEBUG_SESSION_KEY)?.displayName ?: "acton debug"} " +
                    "DAP session on port ${processHandler.getUserData(ACTON_DEBUG_SESSION_KEY)?.port}"
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
                val console = SMTestRunnerConnectionUtil.createConsole(consoleProperties.testFrameworkName, consoleProperties) as SMTRunnerConsoleView
                val handler = startProcess()
                console.attachToProcess(handler)

                val connection = configuration.project.messageBus.connect()
                connection.subscribe(SMTRunnerEventsListener.TEST_STATUS, object : SMTRunnerEventsAdapter() {
                    override fun onBeforeTestingFinished(testsRoot: SMTestProxy.SMRootTestProxy) {
                        testsRoot.setFinished()
                    }
                })

                val smTestProxy = console.resultsViewer.root as SMTestProxy.SMRootTestProxy
                smTestProxy.setTestsReporterAttached()

                return DefaultExecutionResult(console, handler)
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
        if (configuration.command == "test" && environment.executor is CoverageExecutor) {
            args.add("--coverage")
            args.add("--coverage-format")
            args.add("lcov")
        }
        val profilerDump = createProfilerDumpIfNeeded()
        if (profilerDump != null) {
            args.add("--profile-format")
            args.add("collapsed")
            args.add("--cpuprofile")
            args.add(profilerDump.file.absolutePath)
        }

        val actonCommandLine = ActonCommandLine(
            command = actonCommand.name,
            workingDirectory = workingDir,
            additionalArguments = args + additionalArgs,
            environmentVariables = configuration.env
        )

        var commandLine =
            actonCommandLine.toGeneralCommandLine(configuration.project) ?: throw IllegalStateException("Cannot find acton executable")
        if (configuration.emulateTerminal && configuration.command != "test" && !isDebugScript(actonCommand)) {
            commandLine = PtyCommandLine(commandLine)
                .withInitialColumns(PtyCommandLine.MAX_COLUMNS)
                .withConsoleMode(false)
        }

        val handler = KillableColoredProcessHandler(commandLine)
        attachCpuProfileImportIfNeeded(handler, profilerDump)
        attachDebugSessionIfNeeded(handler, commandLine, workingDir, actonCommand)
        return handler
    }

    private fun createActonCommand() = when (val command = configuration.getActonCommand()) {
        is ActonCommand.Script -> {
            val debugPort = scriptDebugPortOverride?.toString() ?: command.debugPort
            command.copy(
                debug = scriptDebugPortOverride != null || command.debug,
                debugPort = debugPort
            )
        }
        is ActonCommand.Test -> {
            val debugPort = testDebugPortOverride?.toString() ?: command.debugPort
            command.copy(
                debug = testDebugPortOverride != null || command.debug,
                debugPort = debugPort
            )
        }
        else -> command
    }

    private fun attachDebugSessionIfNeeded(
        handler: KillableColoredProcessHandler,
        commandLine: GeneralCommandLine,
        workingDir: java.nio.file.Path,
        actonCommand: ActonCommand
    ) {
        val debugInfo = when (actonCommand) {
            is ActonCommand.Script -> {
                if (!isDebugScript(actonCommand)) return
                DebugSessionInfo(
                    displayName = "acton script",
                    port = actonCommand.debugPort.toIntOrNull() ?: return,
                    readinessMarkers = listOf(
                        "Debugger server listening on 127.0.0.1:${actonCommand.debugPort}",
                        "Retrace DAP listening on 127.0.0.1:${actonCommand.debugPort}"
                    )
                )
            }
            is ActonCommand.Test -> {
                if (!isDebugTest(actonCommand)) return
                DebugSessionInfo(
                    displayName = "acton test",
                    port = actonCommand.debugPort.toIntOrNull() ?: return,
                    readinessMarkers = listOf("Debugger server listening on 127.0.0.1:${actonCommand.debugPort}")
                )
            }
            else -> return
        }

        val debugSession = ActonDebugSession(
            displayName = debugInfo.displayName,
            port = debugInfo.port,
            readinessMarkers = debugInfo.readinessMarkers
        )
        debugSession.recordStartup(commandLine.commandLineString, workingDir)
        ActiveActonDebugProcessRegistry.register(
            project = configuration.project,
            workingDir = workingDir,
            processHandler = handler,
            displayName = debugInfo.displayName,
            commandLine = commandLine.commandLineString
        )
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
                ActiveActonDebugProcessRegistry.unregister(
                    project = configuration.project,
                    workingDir = workingDir,
                    processHandler = handler
                )
                debugSession.processTerminated(event.exitCode)
            }
        })
    }

    private fun isDebugScript(actonCommand: ActonCommand): Boolean {
        val scriptCommand = actonCommand as? ActonCommand.Script ?: return false
        return scriptCommand.debug && !scriptCommand.broadcast
    }

    private fun isDebugTest(actonCommand: ActonCommand): Boolean {
        val testCommand = actonCommand as? ActonCommand.Test ?: return false
        return testCommand.debug
    }

    private fun createProfilerDumpIfNeeded(): ProfilerDumpDescriptor? {
        if (configuration.command != "test") return null
        if (environment.executor.id != ProfilerExecutor.EXECUTOR_ID) return null

        val dumpManager = ProfilerDumpManager.getInstance(configuration.project)
        val dumpName = configuration.name.takeIf { it.isNotBlank() } ?: "Acton Test"
        return dumpManager.createDump(dumpName, CPU_PROFILE_PARSER_PROVIDER)
    }

    private fun attachCpuProfileImportIfNeeded(
        handler: KillableColoredProcessHandler,
        profilerDump: ProfilerDumpDescriptor?
    ) {
        if (profilerDump == null) return

        handler.addProcessListener(object : ProcessAdapter() {
            override fun processTerminated(event: ProcessEvent) {
                val dumpFile = profilerDump.file
                if (!dumpFile.exists() || dumpFile.length() == 0L) {
                    LOG.warn("Acton CPU profile was not produced for '${configuration.name}' at ${dumpFile.absolutePath}")
                    profilerDump.remove()
                    return
                }

                LOG.info("Importing Acton CPU profile from ${dumpFile.absolutePath}")
                ApplicationManager.getApplication().invokeLater {
                    if (configuration.project.isDisposed) {
                        profilerDump.remove()
                        return@invokeLater
                    }
                    ImportProfilerResultAction.Companion.importProfilerDump(
                        configuration.project,
                        profilerDump,
                        null,
                        null
                    )
                }
            }
        })
    }

    companion object {
        private val LOG = logger<ActonCommandRunState>()
        private val CPU_PROFILE_PARSER_PROVIDER = ActonCollapsedProfileDumpParserProvider()
    }
}

private data class DebugSessionInfo(
    val displayName: String,
    val port: Int,
    val readinessMarkers: List<String>
)

private object ActiveActonDebugProcessRegistry {
    private val activeProcesses = ConcurrentHashMap<String, ActiveActonDebugProcess>()

    fun register(
        project: Project,
        workingDir: java.nio.file.Path,
        processHandler: ProcessHandler,
        displayName: String,
        commandLine: String
    ) {
        val key = key(project, workingDir)
        val candidate = ActiveActonDebugProcess(processHandler, displayName, commandLine)
        while (true) {
            val existing = activeProcesses[key]
            if (existing == null) {
                if (activeProcesses.putIfAbsent(key, candidate) == null) {
                    return
                }
                continue
            }

            if (existing.processHandler.isProcessTerminating || existing.processHandler.isProcessTerminated) {
                activeProcesses.remove(key, existing)
                continue
            }

            throw ExecutionException(
                "Another ${existing.displayName} session is still running for ${workingDir.toAbsolutePath()} " +
                    "and holds the Acton compilation cache.\n" +
                    "Stop it before starting a new debug session.\n\n" +
                    "Existing command: ${existing.commandLine}"
            )
        }
    }

    fun unregister(
        project: Project,
        workingDir: java.nio.file.Path,
        processHandler: ProcessHandler
    ) {
        val key = key(project, workingDir)
        val existing = activeProcesses[key] ?: return
        if (existing.processHandler === processHandler) {
            activeProcesses.remove(key, existing)
        }
    }

    private fun key(project: Project, workingDir: java.nio.file.Path): String {
        return buildString {
            append(project.locationHash)
            append("::")
            append(workingDir.toAbsolutePath().normalize())
        }
    }
}

private data class ActiveActonDebugProcess(
    val processHandler: ProcessHandler,
    val displayName: String,
    val commandLine: String
)

data class PreparedActonDebugExecution(
    val executionResult: ExecutionResult,
    val processHandler: ProcessHandler,
    val debugSession: ActonDebugSession
)
