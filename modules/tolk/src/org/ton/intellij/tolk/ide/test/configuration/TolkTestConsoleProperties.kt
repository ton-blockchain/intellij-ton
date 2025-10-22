package org.ton.intellij.tolk.ide.test.configuration

import com.intellij.execution.Executor
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties

class TolkTestConsoleProperties(
    configuration: TolkTestConfiguration,
    executor: Executor
) : SMTRunnerConsoleProperties(configuration, "TolkTest", executor) {

    init {
        isUsePredefinedMessageFilter = false
        isIdBasedTestTree = true
        isPrintTestingStartedTime = false
    }

    override fun getTestLocator() = TolkTestLocator
}
