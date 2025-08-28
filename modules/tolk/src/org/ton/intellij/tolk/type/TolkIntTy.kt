package org.ton.intellij.tolk.type

import org.ton.intellij.tolk.type.range.TvmIntRangeSet
import java.math.BigInteger

interface TolkIntTy : TolkPrimitiveTy {
    val range: TvmIntRangeSet

    fun negate(): TolkIntTy

    override fun actualType(): TolkTy = TolkTy.Int

    override fun isSuperType(other: TolkTy): Boolean {
        if (other == TolkTy.Never) return true
        if (other is TolkTyAlias) return isSuperType(other.underlyingType)
        if (other !is TolkIntTy) return false
        return range.contains(other.range)
    }

    override fun join(other: TolkTy): TolkTy {
        if (other.unwrapTypeAlias() == this) return this
        if (other == TolkTy.Never) return this
        if (other !is TolkIntTy) return TolkTyUnion.create(this, other)
        return TolkTy.Int
    }

    override fun canRhsBeAssigned(other: TolkTy): Boolean {
        if (other == this) return true
        if (other is TolkIntNTy) return true
        if (other is TolkTyCoins) return true
        if (other is TolkTyAlias) return canRhsBeAssigned(other.unwrapTypeAlias())
        if (other.actualType() == TolkTy.Int) return true
        return other == TolkTy.Never
    }

    override fun isEquivalentToInner(other: TolkTy): Boolean {
        if (this === other) return true
        if (other is TolkTyAlias) return isEquivalentToInner(other.unwrapTypeAlias())
        return actualType() == other.actualType()
    }

    companion object {
        val VALUES get() = listOf(TolkTy.Int) + TolkIntNTy.VALUES
    }
}

data class TolkConstantIntTy(
    override val value: BigInteger
) : TolkIntTy, TolkConstantTy<BigInteger>, TolkIntTyFamily {
    constructor(value: Long) : this(value.toBigInteger())

    override val range: TvmIntRangeSet get() = TvmIntRangeSet.point(value)

    override fun negate(): TolkIntTy = TolkConstantIntTy(value.negate())

    override fun isSuperType(other: TolkTy): Boolean {
        if (other is TolkTyAlias) return isSuperType(other.underlyingType)
        return other == this || other == TolkTyNever
    }

    override fun canRhsBeAssigned(other: TolkTy): Boolean {
        return super<TolkIntTy>.canRhsBeAssigned(other)
    }

    override fun toString(): String {
        val bitLength = value.bitLength()
        if (bitLength <= 16) return value.toString()
        return "0x${value.toString(16)}"
    }

}

data class TolkIntRangeTy(
    override val range: TvmIntRangeSet
) : TolkIntTy, TolkIntTyFamily {

    override fun negate(): TolkIntTy = TolkIntRangeTy(range.unaryMinus())

    override fun toString(): String = range.toString()

    override fun canRhsBeAssigned(other: TolkTy): Boolean {
        return super<TolkIntTy>.canRhsBeAssigned(other)
    }
}
