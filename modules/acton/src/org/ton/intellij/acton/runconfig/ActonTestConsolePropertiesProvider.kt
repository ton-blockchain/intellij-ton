package org.ton.intellij.acton.runconfig

import com.intellij.execution.Executor
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.openapi.extensions.ExtensionPointName

interface ActonTestConsolePropertiesProvider {
    fun createConsoleProperties(configuration: ActonCommandConfiguration, executor: Executor): SMTRunnerConsoleProperties?
    
    companion object {
        val EP_NAME = ExtensionPointName.create<ActonTestConsolePropertiesProvider>("org.ton.intellij.acton.testConsolePropertiesProvider")
    }
}
