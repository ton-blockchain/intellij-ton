package org.ton.intellij.tolk.type.range

import org.ton.intellij.util.TVM_INT_MAX_VALUE
import org.ton.intellij.util.TVM_INT_MIN_VALUE
import java.math.BigInteger

sealed class TvmIntRangeSet {
    abstract val min: BigInteger

    abstract val max: BigInteger

    fun isEmpty(): Boolean = this == Empty

    open operator fun contains(value: BigInteger): Boolean = contains(Point(value))

    open operator fun contains(value: Long): Boolean = contains(BigInteger.valueOf(value))

    abstract operator fun contains(value: TvmIntRangeSet): Boolean

    abstract fun join(other: TvmIntRangeSet): TvmIntRangeSet

    abstract operator fun unaryMinus(): TvmIntRangeSet

    abstract operator fun minus(other: TvmIntRangeSet): TvmIntRangeSet

    protected abstract fun asRangeArray(): Array<BigInteger>

    data class Range(
        override val min: BigInteger,
        override val max: BigInteger
    ) : TvmIntRangeSet() {
        override fun asRangeArray(): Array<BigInteger> = arrayOf(min, max)

        override fun contains(value: BigInteger): Boolean = value in min..max
        override fun contains(value: Long): Boolean = value in min.toLong()..max.toLong()
        override fun contains(value: TvmIntRangeSet): Boolean =
            value.isEmpty() || (min <= value.min && value.max <= max)

        override fun unaryMinus(): TvmIntRangeSet = Range(-max, -min)

        override fun minus(other: TvmIntRangeSet): TvmIntRangeSet {
            return when (other) {
                Empty -> this
                this -> Empty
                is Point -> {
                    val value = other.value
                    if (value < min || value > max) return this
                    if (min == value) return range(min + BigInteger.ONE, max)
                    if (max == value) return range(min, max - BigInteger.ONE)
                    RangeSet(arrayOf(min, value - BigInteger.ONE, value + BigInteger.ONE, max))
                }

                is Range -> {
                    var toJoin: TvmIntRangeSet = Empty
                    val otherMin = other.min
                    val otherMax = other.max
                    if (otherMax < min || otherMin > max) return this

                    // TODO: modrange
                    if (otherMin <= min && otherMax >= max) return toJoin
                    if (otherMin > min && otherMax < max) {
                        return RangeSet(arrayOf(min, min - BigInteger.ONE, max + BigInteger.ONE, max)).join(toJoin)
                    }
                    if (otherMin <= min) {
                        return range(max + BigInteger.ONE, max).join(toJoin)
                    }
                    range(min, otherMax - BigInteger.ONE).join(toJoin)
                }

                is RangeSet -> {
                    val ranges = other.ranges
                    var result: TvmIntRangeSet = this
                    for (i in 0 until ranges.size step 2) {
                        result -= range(ranges[i], ranges[i + 1])
                        if (result.isEmpty()) break
                    }
                    result
                }
            }
        }

        override fun join(other: TvmIntRangeSet): TvmIntRangeSet {
            try {
                if (other.isEmpty() || other == this) return this
                if (other is Point) return other.join(this)
                if (other is Range) {
                    if (other.min <= max && min <= other.max ||
                        (other.max < min && other.max + BigInteger.ONE == min) ||
                        (other.min > max && max + BigInteger.ONE == other.min)
                    ) {
                        return range(minOf(min, other.min), maxOf(max, other.max))
                    }
                    if (other.max < min) {
                        return RangeSet(other.min, other.max, min, max)
                    }
                    return RangeSet(min, max, other.min, other.max)
                }

                val longs = other.asRangeArray()
                var minIndex = longs.binarySearch(min)
                if (minIndex < 0) {
                    minIndex = -minIndex - 1
                    if (minIndex % 2 == 0 && minIndex > 0 && longs[minIndex - 1] + BigInteger.ONE == min) {
                        minIndex--
                    }
                } else if (minIndex % 2 == 0) {
                    minIndex++
                }
                var maxIndex = longs.binarySearch(max)
                if (maxIndex < 0) {
                    maxIndex = -maxIndex - 1
                    if (maxIndex % 2 == 0 && maxIndex < longs.size && max + BigInteger.ONE == longs[maxIndex]) {
                        maxIndex++
                    }
                } else if (maxIndex % 2 == 0) {
                    maxIndex++
                }

                val result = Array(longs.size + 2) { BigInteger.ZERO }
                System.arraycopy(longs, 0, result, 0, minIndex)
                var pos = minIndex
                if (minIndex % 2 == 0) {
                    result[pos++] = min
                }
                if (maxIndex % 2 == 0) {
                    result[pos++] = max
                }
                val remaining = longs.size - maxIndex
                if (remaining > 0) {
                    System.arraycopy(longs, maxIndex, result, pos, remaining)
                }

                return ranges(result, longs.size + remaining)
            } catch (e: Exception) {
                throw IllegalStateException("Failed to join $this with $other", e)
            }
        }

        override fun toString(): String = "{${format(min, max)}}"
    }

    data class Point(
        val value: BigInteger
    ) : TvmIntRangeSet() {
        constructor(value: Long) : this(BigInteger.valueOf(value))

        override val min: BigInteger get() = value
        override val max: BigInteger get() = value

        override fun asRangeArray(): Array<BigInteger> = arrayOf(value, value)

        override fun contains(value: TvmIntRangeSet): Boolean = value.isEmpty() || this == value
        override fun contains(value: BigInteger): Boolean = this.value == value

        override fun minus(other: TvmIntRangeSet): TvmIntRangeSet {
            return if (other.contains(value)) Empty else this
        }

        override fun unaryMinus(): TvmIntRangeSet = Point(-value)

        override fun join(other: TvmIntRangeSet): TvmIntRangeSet {
            if (other.isEmpty()) return this
            if (other.contains(value)) return other
            if (other is Point) {
                val min = minOf(value, other.value)
                val max = maxOf(value, other.value)
                return if (min + BigInteger.ONE == max) range(min, max) else RangeSet(
                    arrayOf(min, min, max, max)
                )
            }
            if (other is Range) {
                return if (value < other.min) {
                    if (value + BigInteger.ONE == other.min) {
                        range(value, other.max)
                    } else {
                        RangeSet(value, value, other.min, other.max)
                    }
                } else {
                    if (value - BigInteger.ONE == other.max) {
                        range(other.min, value)
                    } else {
                        RangeSet(other.min, other.max, value, value)
                    }
                }
            }
            val longs = other.asRangeArray()
            val pos = -longs.binarySearch(value) - 1
            val touchLeft = pos > 0 && longs[pos - 1] + BigInteger.ONE == value
            val touchRight = pos < longs.size - 1 && value + BigInteger.ONE == longs[pos]
            val result = if (touchLeft) {
                if (touchRight) {
                    val result = Array(longs.size - 2) { BigInteger.ZERO }
                    System.arraycopy(longs, 0, result, 0, pos - 1)
                    System.arraycopy(longs, pos + 1, result, pos - 1, longs.size - pos - 1)
                    result
                } else {
                    longs[pos - 1] = value
                    longs
                }
            } else {
                if (touchRight) {
                    longs[pos] = value
                    longs
                } else {
                    val result = Array(longs.size + 2) { BigInteger.ZERO }
                    System.arraycopy(longs, 0, result, 0, pos)
                    result[pos] = value
                    System.arraycopy(longs, pos, result, pos + 2, longs.size - pos)
                    result
                }
            }
            return ranges(result, result.size)
        }

        override fun toString(): String = "{${value.format()}}"

        companion object {
            val ZERO = Point(BigInteger.ZERO)
            val ONE = Point(BigInteger.ONE)
        }
    }

    class RangeSet(
        val ranges: Array<BigInteger>
    ) : TvmIntRangeSet() {
        constructor(r1min: BigInteger, r1max: BigInteger, r2min: BigInteger, r2max: BigInteger) : this(
            arrayOf(
                r1min,
                r1max,
                r2min,
                r2max
            )
        )

        override val min: BigInteger get() = ranges.first()
        override val max: BigInteger get() = ranges.last()

        override fun asRangeArray(): Array<BigInteger> = ranges.copyOf()

        override fun contains(value: BigInteger): Boolean {
            for (i in 0 until ranges.size step 2) {
                if (value >= ranges[i] && value <= ranges[i + 1]) {
                    return true
                }
            }
            return false
        }

        override fun contains(value: TvmIntRangeSet): Boolean {
            if (value.isEmpty() || value == this) return true
            if (value is Point) return contains(value.value)
            var result = value
            for (i in 0 until ranges.size step 2) {
                result -= range(ranges[i], ranges[i + 1])
                if (result.isEmpty()) return true
            }
            return false
        }

        override fun join(other: TvmIntRangeSet): TvmIntRangeSet {
            if (other !is RangeSet) return other.join(this)
            if (other == this) return this
            if (other.contains(this)) return other
            if (this.contains(other)) return this
            var result = other
            for (i in 0 until ranges.size step 2) {
                result = range(ranges[i], ranges[i + 1]).join(result)
            }
            return result
        }

        override fun unaryMinus(): TvmIntRangeSet {
            var result: TvmIntRangeSet = Empty
            for (i in 0 until ranges.size step 2) {
                result = result.join(range(ranges[i], ranges[i + 1]).unaryMinus())
            }
            return result
        }

        override fun minus(other: TvmIntRangeSet): TvmIntRangeSet {
            if (other.isEmpty()) return this
            if (other == this) return Empty
            val result = Array(ranges.size + other.asRangeArray().size) { BigInteger.ZERO }
            var index = 0
            for (i in 0 until ranges.size step 2) {
                val res = range(ranges[i], ranges[i + 1]) - other
                val ranges = res.asRangeArray()
                System.arraycopy(ranges, 0, result, index, ranges.size)
                index += ranges.size
            }
            return ranges(result, index)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RangeSet) return false
            return ranges.contentEquals(other.ranges)
        }

        override fun hashCode(): Int = ranges.contentHashCode()
    }

    object Empty : TvmIntRangeSet() {
        override val min: BigInteger
            get() = throw NoSuchElementException()
        override val max: BigInteger
            get() = throw NoSuchElementException()

        override fun asRangeArray(): Array<BigInteger> = emptyArray()

        override fun contains(value: BigInteger): Boolean = false
        override fun contains(value: Long): Boolean = false
        override fun contains(value: TvmIntRangeSet): Boolean = false

        override fun minus(other: TvmIntRangeSet): TvmIntRangeSet = this

        override fun join(other: TvmIntRangeSet): TvmIntRangeSet = other

        override fun unaryMinus(): TvmIntRangeSet = this

        override fun toString(): String = "{}"
    }

    companion object {
        val MIN = point(TVM_INT_MIN_VALUE)
        val MAX = point(TVM_INT_MAX_VALUE)

        val ALL = range(TVM_INT_MIN_VALUE, TVM_INT_MAX_VALUE)

        fun point(value: Long): Point = Point(value)
        fun point(value: BigInteger): Point = Point(value)

        fun range(from: BigInteger, to: BigInteger): TvmIntRangeSet {
            return if (from == to) point(from)
            else Range(from, to)
        }

        private fun ranges(ranges: Array<BigInteger>, bound: Int): TvmIntRangeSet {
            if (bound == 0) return Empty
            if (bound == 2) return range(ranges[0], ranges[1])
            return RangeSet(ranges.copyOfRange(0, bound))
        }
    }
}

private fun format(min: BigInteger, max: BigInteger): String {
    return min.format() + (if (min == max) "" else if (max - min == BigInteger.ONE) "," else "..") + max.format()
}

private fun BigInteger.format(): String {
    return when {
        this == TVM_INT_MAX_VALUE -> "MAX"
        this == TVM_INT_MAX_VALUE - BigInteger.ONE -> "MAX-1"
        this == TVM_INT_MIN_VALUE -> "MIN"
        this == TVM_INT_MIN_VALUE + BigInteger.ONE -> "MIN+1"
        else -> toString()
    }
}
