package org.ton.intellij.tvm.math

import java.math.BigInteger

val TVM_INT_MIN_VALUE = -(BigInteger.valueOf(2).pow(256))
val TVM_INT_MAX_VALUE = (BigInteger.valueOf(2).pow(256)) - BigInteger.ONE

enum class RoundMode {
    Floor,
    Round,
    Ceil,
    FloorToZero;

    fun divMod(a: BigInteger, b: BigInteger): Pair<BigInteger, BigInteger> = when (this) {
        Floor -> a.divModFloor(b)
        Round -> a.divModRound(b)
        Ceil -> a.divModCeil(b)
        FloorToZero -> a.divModFloorToZero(b)
    }

    fun div(a: BigInteger, b: BigInteger): BigInteger = divMod(a, b).first

    fun mod(a: BigInteger, b: BigInteger): BigInteger = divMod(a, b).second
}

fun BigInteger.divModFloor(other: BigInteger): Pair<BigInteger, BigInteger> {
    val (d, r) = this.divideAndRemainder(other)
    if (this.signum() == other.signum()) {
        return d to r
    }
    return if (r == BigInteger.ZERO) {
        d to r
    } else {
        d - BigInteger.ONE to other - r
    }
}

fun BigInteger.divModRound(other: BigInteger): Pair<BigInteger, BigInteger> {
    val (d, r) = this.divideAndRemainder(other)
    val half = other / BigInteger.TWO
    return if (r.abs() > half) {
        if (d.signum() == r.signum()) {
            d + BigInteger.ONE to r - other
        } else {
            d - BigInteger.ONE to r + other
        }
    } else {
        d to r
    }
}

fun BigInteger.divModCeil(other: BigInteger): Pair<BigInteger, BigInteger> {
    val (d, r) = this.divideAndRemainder(other)
    if (d.signum() == r.signum()) {
        return d to r
    }
    return if (r == BigInteger.ZERO) {
        d to r
    } else {
        d + BigInteger.ONE to r - other
    }
}

fun BigInteger.divModFloorToZero(other: BigInteger): Pair<BigInteger, BigInteger> {
    val (d, r) = this.divideAndRemainder(other)
    return d to r
}
