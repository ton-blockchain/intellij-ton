package org.ton.intellij.tolk.ide.run.configuration

import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.project.guessProjectDir
import com.intellij.util.execution.ParametersListUtil
import org.ton.intellij.tolk.ide.configurable.tolkSettings

class TolkRunScriptConfigurationRunState(
    val env: ExecutionEnvironment,
    private val conf: TolkRunScriptConfiguration,
) : RunProfileState {

    override fun execute(exec: Executor?, runner: ProgramRunner<*>) = startProcess(exec, runner)

    private fun startProcess(exec: Executor?, runner: ProgramRunner<*>): ExecutionResult? {
        if (exec == null) {
            return null
        }

        val state = object : CommandLineState(env) {
            override fun startProcess(): ProcessHandler {
                val workingDir = conf.project.guessProjectDir()?.toNioPath()?.toFile()
                    ?: throw IllegalStateException("Can't run script, cannot locate working dir")
                val testToolPath = conf.project.tolkSettings.testToolPath
                    ?: throw IllegalStateException("Can't run script, Acton binary is not configured")

                val commandLine = GeneralCommandLine()
                    .withExePath(testToolPath)
                    .withWorkDirectory(workingDir)
                    .withParameters("script")
                    .withParameters(conf.filename)

                val additionalArguments = ParametersListUtil.parse(conf.additionalParameters)
                commandLine.addParameters(additionalArguments)

                return KillableColoredProcessHandler(commandLine)
            }
        }

        return state.execute(exec, runner)
    }
}
