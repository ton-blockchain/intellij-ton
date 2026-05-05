package org.ton.intellij.tolk.ide.test

import com.intellij.execution.TestStateStorage

internal object TolkTestStateMagnitude {
    private const val PASSED_OR_COMPLETE = 1
    private const val FAILED = 6
    private const val ERROR = 8

    fun isPassed(record: TestStateStorage.Record): Boolean = record.magnitude == PASSED_OR_COMPLETE

    fun isFailure(record: TestStateStorage.Record): Boolean = record.magnitude == ERROR || record.magnitude == FAILED
}
