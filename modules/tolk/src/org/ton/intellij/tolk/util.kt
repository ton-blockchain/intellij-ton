package org.ton.intellij.tolk

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTimedValue

fun <T> perf(string: String, block: () -> T): T {
    val (value, time) = measureTimedValue {
        block()
    }
    if (time > 50.milliseconds) {
        println("$string took $time")
    }
    return value
}
