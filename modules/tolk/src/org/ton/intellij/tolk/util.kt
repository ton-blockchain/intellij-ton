package org.ton.intellij.tolk

import com.intellij.openapi.diagnostic.Logger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTimedValue

private val LOG = Logger.getInstance("TolkPerformance")

fun <T> perf(string: String, moreThan: Duration = 50.milliseconds, block: () -> T): T {
    val (value, time) = measureTimedValue {
        block()
    }
    if (time > moreThan) {
        LOG.warn("$string took $time")
    }
    return value
}
