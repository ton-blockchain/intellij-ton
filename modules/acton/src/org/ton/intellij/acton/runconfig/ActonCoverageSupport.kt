package org.ton.intellij.acton.runconfig

import com.intellij.execution.Executor

const val ACTON_COVERAGE_EXECUTOR_ID: String = "Coverage"
private const val COVERAGE_DATA_MANAGER_CLASS = "com.intellij.coverage.CoverageDataManager"

const val ACTON_COVERAGE_UNSUPPORTED_MESSAGE: String =
    "Coverage is not supported in this IDE because the IntelliJ coverage API is unavailable. Use Run/Debug instead, or open the project in an IDE build with coverage support."

fun Executor.isActonCoverageExecutor(): Boolean = id == ACTON_COVERAGE_EXECUTOR_ID

fun isActonCoverageSupported(): Boolean = isClassAvailable(COVERAGE_DATA_MANAGER_CLASS)

private fun isClassAvailable(className: String): Boolean {
    return runCatching {
        Class.forName(className, false, ActonCoverageSupport::class.java.classLoader)
    }.isSuccess
}

private object ActonCoverageSupport
