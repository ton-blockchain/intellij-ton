package org.ton.intellij.tolk

import com.intellij.openapi.diagnostic.Logger
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTimedValue


@OptIn(ExperimentalContracts::class)
inline fun <T> perf(string: String, moreThan: Duration = 50.milliseconds, block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val (value, time) = measureTimedValue {
        block()
    }
    if (time > moreThan) {
        Performance.LOG.warn("$string took $time")
    }
    return value
}

object Performance {
    val LOG = Logger.getInstance("TolkPerformance")
}
