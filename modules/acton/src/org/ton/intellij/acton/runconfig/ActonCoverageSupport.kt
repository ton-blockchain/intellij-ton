package org.ton.intellij.acton.runconfig

import com.intellij.execution.Executor

const val ACTON_COVERAGE_EXECUTOR_ID: String = "Coverage"

const val ACTON_COVERAGE_UNSUPPORTED_MESSAGE: String =
    "Coverage is not supported in this IDE because the IntelliJ coverage API is unavailable. Use Run/Debug instead, or open the project in an IDE build with coverage support."

fun Executor.isActonCoverageExecutor(): Boolean = id == ACTON_COVERAGE_EXECUTOR_ID

fun isActonCoverageSupported(): Boolean {
    try {
        // can throw on class load fail
        @Suppress("SimplifyBooleanWithConstants", "KotlinConstantConditions")
        return com.intellij.coverage.CoverageExecutor.EXECUTOR_ID == ACTON_COVERAGE_EXECUTOR_ID
    } catch (_: Exception) {
        return false
    }
}
