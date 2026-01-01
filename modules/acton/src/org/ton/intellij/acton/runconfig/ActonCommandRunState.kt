package org.ton.intellij.acton.runconfig

import com.intellij.coverage.CoverageExecutor
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.project.guessProjectDir
import com.intellij.util.execution.ParametersListUtil
import org.ton.intellij.acton.cli.ActonCommandLine

class ActonCommandRunState(
    environment: ExecutionEnvironment,
    private val configuration: ActonCommandConfiguration,
) : CommandLineState(environment) {

    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
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

        val actonCommand = configuration.getActonCommand()
        val additionalArgs = ParametersListUtil.parse(configuration.parameters)

        val args = actonCommand.getArguments().toMutableList()
        if (configuration.command == "test" && environment.executor is CoverageExecutor) {
            args.add("--coverage")
            args.add("--coverage-format")
            args.add("lcov")
        }

        val actonCommandLine = ActonCommandLine(
            command = actonCommand.name,
            workingDirectory = workingDir,
            additionalArguments = args + additionalArgs,
            environmentVariables = configuration.env
        )

        var commandLine = actonCommandLine.toGeneralCommandLine(configuration.project)
        if (configuration.emulateTerminal && configuration.command != "test") {
            commandLine = PtyCommandLine(commandLine)
                .withInitialColumns(PtyCommandLine.MAX_COLUMNS)
                .withConsoleMode(false)
        }

        val handler = KillableColoredProcessHandler(commandLine)
        return handler
    }
}
