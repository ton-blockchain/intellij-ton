package org.ton.intellij.tolk.type

import org.ton.intellij.tolk.type.range.TvmIntRangeSet
import java.math.BigInteger

interface TolkIntTy : TolkPrimitiveTy {
    val range: TvmIntRangeSet

    fun negate(): TolkIntTy

    override fun actualType(): TolkTy = TolkTy.Int

    override fun isSuperType(other: TolkTy): Boolean {
        if (other == TolkTy.Never) return true
        if (other is TolkTypeAliasTy) return isSuperType(other.underlyingType)
        if (other !is TolkIntTy) return false
        return range.contains(other.range)
    }

    override fun join(other: TolkTy): TolkTy {
        if (other == this) return this
        if (other == TolkTy.Never) return this
        if (other is TolkTypeAliasTy) return join(other.underlyingType)
        if (other !is TolkIntTy) return TolkUnionTy.create(this, other)
        val range = this.range.join(other.range)
        if (range is TvmIntRangeSet.Point) return TolkConstantIntTy(range.value)
        return TolkIntRangeTy(range)
    }

    override fun canRhsBeAssigned(other: TolkTy): Boolean {
        if (other == this) return true
        if (other is TolkIntNTy) return true
        if (other is TolkCoinsTy) return true
        if (other is TolkTypeAliasTy) return canRhsBeAssigned(other.unwrapTypeAlias())
        if (other.actualType() == TolkTy.Int) return true
        return other == TolkTy.Never
    }

    override fun isEquivalentToInner(other: TolkTy): Boolean {
        if (this === other) return true
        return actualType() == other.actualType()
    }
}

data class TolkConstantIntTy(
    override val value: BigInteger
) : TolkIntTy, TolkConstantTy<BigInteger> {
    constructor(value: Long) : this(value.toBigInteger())

    override val range: TvmIntRangeSet get() = TvmIntRangeSet.point(value)

    override fun negate(): TolkIntTy = TolkConstantIntTy(value.negate())

    override fun isSuperType(other: TolkTy): Boolean {
        if (other is TolkTypeAliasTy) return isSuperType(other.underlyingType)
        return other == this || other == TolkNeverTy
    }

    override fun toString(): String {
        val bitLength = value.bitLength()
        if (bitLength <= 16) return value.toString()
        return "0x${value.toString(16)}"
    }

}

data class TolkIntRangeTy(
    override val range: TvmIntRangeSet
) : TolkIntTy {

    override fun negate(): TolkIntTy = TolkIntRangeTy(range.unaryMinus())

    override fun toString(): String = range.toString()
}
