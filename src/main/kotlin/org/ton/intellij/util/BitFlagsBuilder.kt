package org.ton.intellij.util

abstract class BitFlagsBuilder private constructor(private val limit: Limit, startFromBit: Int) {
    protected constructor(limit: Limit) : this(limit, 0)
    protected constructor(prevBuilder: BitFlagsBuilder, limit: Limit) : this(limit, prevBuilder.counter)

    private var counter: Int = startFromBit

    protected fun nextBitMask(): Int {
        val nextBit = counter++
        if (nextBit == limit.bits) error("Bitmask index out of $limit limit!")
        return 1 shl nextBit
    }

    protected enum class Limit(val bits: Int) {
        BYTE(8), INT(32)
    }
}
