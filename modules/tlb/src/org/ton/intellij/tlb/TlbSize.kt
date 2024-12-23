package org.ton.intellij.tlb

import kotlin.math.max
import kotlin.math.min

class TlbSize private constructor(
    val value: Long
) {
    constructor(
        minRefs: Int,
        minBits: Int,
        maxRefs: Int,
        maxBits: Int
    ) : this(
        ((minBits.toLong() * 0x100 + minRefs.toLong()) shl 32) + (maxBits.toLong() * 0x100 + maxRefs.toLong())
    )

    val minSize: Int get() = (value ushr 32).toInt()
    val maxSize: Int get() = (value and 0xFFFFFFFFL).toInt()

    // |
    val maxRefs: Int get() = (value and 0xFF).toInt()
    val maxBits: Int get() = ((value ushr 8) and BITS_MASK).toInt()
    val minRefs: Int get() = ((value ushr 32) and 0xFF).toInt()
    val minBits: Int get() = ((value ushr 40) and BITS_MASK).toInt()

    val isFixed: Boolean get() = minSize == maxSize

    fun fitsIntoCell(): Boolean =
        isPossible(MAX_SIZE_CELL, minSize)

    fun isPossible(): Boolean =
        isPossible(maxSize, minSize)

    fun normalize(): TlbSize {
        return TlbSize(normalize(value))
    }

    fun withoutMin(): TlbSize = TlbSize(value and ((1L shl 32) - 1))

    operator fun plus(other: TlbSize): TlbSize {
        return TlbSize(normalize(value + other.value))
    }

    infix fun or(other: TlbSize): TlbSize {
        return TlbSize(
            minRefs = min(minRefs, other.minRefs),
            minBits = min(minBits, other.minBits),
            maxRefs = max(maxRefs, other.maxRefs),
            maxBits = max(maxBits, other.maxBits)
        )
    }

    operator fun times(count: Int): TlbSize {
        return when (count) {
            0 -> TlbSize(0)
            1 -> this
            else -> TlbSize(
                min(minRefs * count, MAX_REFS_MASK.toInt()),
                min(minBits * count, BITS_MASK.toInt()),
                min(maxRefs * count, MAX_REFS_MASK.toInt()),
                min(maxBits * count, BITS_MASK.toInt())
            )
        }
    }

    fun timesAtLeast(count: Int): TlbSize {
        val clampedCount = min(max(count, 0), 1024)
        return TlbSize(
            minRefs = min(minRefs * clampedCount, MAX_REFS_MASK.toInt()),
            minBits = min(minBits * clampedCount, BITS_MASK.toInt()),
            maxRefs = if (maxRefs != 0) MAX_REFS_MASK.toInt() else 0,
            maxBits = if (maxBits != 0) BITS_MASK.toInt() else 0
        )
    }

    override fun toString(): String = buildString {
        fun appendSize(bits: Int, refs: Int) {
            if (bits >= 1024 && refs >= 7) {
                append("Inf")
            } else {
                append(bits)
                if (refs > 0) {
                    append("+")
                    append(refs)
                    append("R")
                }
            }
        }

        if (isFixed) {
            append("=")
        }
        appendSize(minBits, minRefs)
        if (!isFixed) {
            append("..")
            appendSize(maxBits, maxRefs)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TlbSize) return false
        if (value != other.value) return false
        return true
    }

    override fun hashCode(): Int = value.hashCode()

    companion object {
        const val MAX_SIZE_CELL: Int = 0x3FF04 // 0x3FF = 1023 bits, 0x04 = 4 refs
        const val BITS_MASK: Long = 0x7FF
        const val MAX_REFS_MASK: Long = 7

        val ONE_REF: TlbSize = TlbSize(0x100000001)
        val ANY: TlbSize = TlbSize(0x7ff07)
        val IMPOSSIBLE: TlbSize = TlbSize(0x7ff07L shl 32)
        val ZERO: TlbSize = TlbSize(0)

        fun convertSize(z: Int): Int = ((z and 0xFF) shl 16) or (z ushr 8)

        private fun isPossible(
            maxSize: Int,
            minSize: Int
        ): Boolean = (maxSize - minSize).toLong() and 0x80000080 == 0L

        private fun normalize(value: Long): Long {
            var v = value
            v = normalize(v, 0xF8, 7)
            v = normalize(v, 0xfff80000, 0x7ff00)
            v = normalize(v, 0xF8L shl 32, 7L shl 32)
            v = normalize(v, 0xfff80000L shl 32, 0x7ff00L shl 32)
            return v
        }

        private fun normalize(value: Long, a: Long, b: Long): Long {
            if (value and a != 0L) {
                return (value or (a or b)) - a
            }
            return value
        }

        fun fixedSize(size: Int): TlbSize = TlbSize(size.toLong() * 0x10000000100)

        fun range(minSize: Int, maxSize: Int): TlbSize =
            TlbSize(((minSize.toLong() shl 32) + maxSize) shl 8)
    }
}
