package org.ton.intellij.acton.ide

import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionUtil
import org.ton.intellij.acton.runconfig.ActonCommandConfiguration
import org.ton.intellij.acton.runconfig.ActonCommandConfigurationType

object ActonBacktraceRerunLauncher {
    fun launch(
        sourceConfiguration: ActonCommandConfiguration,
        failedTestName: String? = null,
        testTarget: String? = null,
    ): Boolean {
        if (sourceConfiguration.command != "test") {
            return false
        }

        val project = sourceConfiguration.project
        val runManager = RunManager.getInstance(project)
        val settings = runManager.createConfiguration(
            buildConfigurationName(sourceConfiguration, failedTestName),
            ActonCommandConfigurationType.getInstance().factory
        )
        val configuration = settings.configuration as ActonCommandConfiguration
        configuration.copyFrom(sourceConfiguration)
        configuration.parameters = withBacktraceFull(sourceConfiguration.parameters)

        failedTestName?.trim()?.takeIf { it.isNotBlank() }?.let { testName ->
            configuration.testMode = org.ton.intellij.acton.cli.ActonCommand.Test.TestMode.FUNCTION
            configuration.testFunctionName = testName
            testTarget?.trim()?.takeIf { it.isNotBlank() }?.let { configuration.testTarget = it }
        }

        runManager.addConfiguration(settings)
        runManager.selectedConfiguration = settings
        ExecutionUtil.runConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance())
        return true
    }

    internal fun withBacktraceFull(parameters: String): String {
        val trimmed = parameters.trim()
        if (trimmed.isBlank()) {
            return "--backtrace full"
        }
        if (FULL_BACKTRACE_REGEX.containsMatchIn(trimmed)) {
            return trimmed
        }
        if (BACKTRACE_REGEX.containsMatchIn(trimmed)) {
            return BACKTRACE_REGEX.replace(trimmed) { match ->
                val prefix = match.groups[1]?.value.orEmpty()
                "${prefix}--backtrace full"
            }
        }
        return "$trimmed --backtrace full"
    }

    private fun buildConfigurationName(configuration: ActonCommandConfiguration, failedTestName: String?): String {
        val suffix = failedTestName?.takeIf { it.isNotBlank() }?.let { " [$it backtrace]" } ?: " [backtrace]"
        return configuration.name + suffix
    }

    private val FULL_BACKTRACE_REGEX = Regex("""(?:^|\s)--backtrace\s+full(?:\s|$)""")
    private val BACKTRACE_REGEX = Regex("""(^|\s)--backtrace\s+\S+(?=\s|$)""")
}
