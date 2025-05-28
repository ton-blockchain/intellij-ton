package org.ton.intellij.tolk

import com.intellij.openapi.diagnostic.Logger
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTimedValue

private val LOG = Logger.getInstance("TolkPerformance")

fun <T> perf(string: String, block: () -> T): T {
    val (value, time) = measureTimedValue {
        block()
    }
    if (time > 50.milliseconds) {
        LOG.warn("$string took $time")
    }
    return value
}
