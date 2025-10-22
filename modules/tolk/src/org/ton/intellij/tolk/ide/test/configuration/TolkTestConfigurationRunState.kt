package org.ton.intellij.tolk.ide.test.configuration

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.openapi.project.guessProjectDir
import com.intellij.util.execution.ParametersListUtil
import org.ton.intellij.tolk.ide.configurable.tolkSettings
import java.io.File

class TolkTestConfigurationRunState(
    env: ExecutionEnvironment,
    private val conf: TolkTestConfiguration,
) : RunProfileState {

    override fun execute(exec: Executor?, runner: ProgramRunner<*>): ExecutionResult? {
        return startProcess(exec)
    }

    private fun startProcess(exec: Executor?): ExecutionResult? {
        if (exec == null) {
            return null
        }

        val workingDir = conf.project.guessProjectDir()?.toNioPath()?.toFile() ?: return null
        val testToolPath = conf.project.tolkSettings.testToolPath ?: return null

        val commandLine = GeneralCommandLine()
            .withExePath(testToolPath)
            .withWorkDirectory(workingDir)
            .withParameters("test")
            .withParameters("--teamcity")

        when (conf.scope) {
            TolkTestScope.Directory -> {
                commandLine.withParameters(conf.directory)
            }
            TolkTestScope.File -> {
                commandLine.withParameters(conf.filename)
            }
            TolkTestScope.Function -> {
                commandLine.withParameters(conf.filename)
                if (conf.pattern.isNotEmpty()) {
                    commandLine.withParameters("--filter")
                    commandLine.withParameters(conf.pattern.removeSurrounding("`"))
                }
            }
        }

        val additionalArguments = ParametersListUtil.parse(conf.additionalParameters)
        commandLine.addParameters(additionalArguments)

        val consoleProperties = TolkTestConsoleProperties(conf, exec)
        val console = SMTestRunnerConnectionUtil
            .createConsole(
                consoleProperties.testFrameworkName,
                consoleProperties
            ) as SMTRunnerConsoleView

        val handler = KillableColoredProcessHandler(commandLine)
        console.attachToProcess(handler)

        val connection = conf.project.messageBus.connect()
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
