package org.ton.intellij.tolk.ide.test.configuration

import com.intellij.execution.Executor
import com.intellij.execution.testframework.Printer
import com.intellij.execution.testframework.sm.SMStacktraceParser
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.execution.testframework.sm.runner.ui.TestStackTraceParser
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project

class TolkTestConsoleProperties(
    configuration: TolkTestConfiguration,
    executor: Executor
) : SMTRunnerConsoleProperties(configuration, "TolkTest", executor), SMStacktraceParser {

    init {
        isUsePredefinedMessageFilter = false
        isIdBasedTestTree = true
        isPrintTestingStartedTime = false
    }

    override fun getTestStackTraceParser(url: String, proxy: SMTestProxy, project: Project): TestStackTraceParser? {
        return object : TestStackTraceParser(0, null, proxy.errorMessage, proxy.stacktrace) {}
    }

    override fun getTestLocator() = TolkTestLocator

    override fun printExpectedActualHeader(printer: Printer, expected: String, actual: String) {
        printer.print("\n", ConsoleViewContentType.ERROR_OUTPUT)
        printer.print("Actual:   ", ConsoleViewContentType.SYSTEM_OUTPUT)
        printer.print("$actual\n", ConsoleViewContentType.ERROR_OUTPUT)
        printer.print("Expected: ", ConsoleViewContentType.SYSTEM_OUTPUT)
        printer.print(expected, ConsoleViewContentType.ERROR_OUTPUT)
    }

    override fun createRerunFailedTestsAction(consoleView: ConsoleView): TolkRerunFailedTestsAction {
        return TolkRerunFailedTestsAction(consoleView, this)
    }
}
