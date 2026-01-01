package org.ton.intellij.tolk.ide.test.configuration

import com.intellij.execution.Executor
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import org.ton.intellij.acton.runconfig.ActonCommandConfiguration
import org.ton.intellij.acton.runconfig.ActonTestConsolePropertiesProvider

class TolkActonTestConsolePropertiesProvider : ActonTestConsolePropertiesProvider {
    override fun createConsoleProperties(
        configuration: ActonCommandConfiguration,
        executor: Executor
    ): SMTRunnerConsoleProperties? {
        if (configuration.command == "test") {
            return TolkTestConsoleProperties(configuration, executor)
        }
        return null
    }
}
