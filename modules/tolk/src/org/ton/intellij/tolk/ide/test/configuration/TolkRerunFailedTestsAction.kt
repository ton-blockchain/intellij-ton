package org.ton.intellij.tolk.ide.test.configuration

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.project.Project
import org.ton.intellij.acton.cli.ActonCommand
import org.ton.intellij.acton.runconfig.ActonCommandConfiguration
import org.ton.intellij.acton.runconfig.ActonCommandRunState

class TolkRerunFailedTestsAction(
    consoleView: ConsoleView,
    consoleProperties: TestConsoleProperties
) : AbstractRerunFailedTestsAction(consoleView) {

    init {
        init(consoleProperties)
    }

    override fun getRunProfile(environment: ExecutionEnvironment): MyRunProfile {
        val configuration = myConsoleProperties.configuration as ActonCommandConfiguration
        return TolkRerunProfile(configuration, getFailedTestPatterns(configuration.project))
    }

    private fun getFailedTestPatterns(project: Project): List<String> {
        val result = mutableSetOf<String>()
        val failedTests = getFailedTests(project)

        for (failedTest in failedTests) {
            result.add(failedTest.name)
        }

        return result.toList()
    }

    private class TolkRerunProfile(
        private val conf: ActonCommandConfiguration,
        private val failed: List<String>
    ) : MyRunProfile(conf) {

        override fun getState(exec: Executor, env: ExecutionEnvironment): RunProfileState? {
            conf.testTarget = failed.joinToString("|")
            conf.testMode = ActonCommand.Test.TestMode.FUNCTION
            return ActonCommandRunState(env, conf)
        }
    }
}
