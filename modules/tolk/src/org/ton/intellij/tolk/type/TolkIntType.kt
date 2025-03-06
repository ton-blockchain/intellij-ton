package org.ton.intellij.tolk.type

import org.ton.intellij.tolk.type.range.TvmIntRangeSet
import java.math.BigInteger

interface TolkIntType : TolkPrimitiveType {
    val range: TvmIntRangeSet

    fun negate(): TolkIntType

    override fun isSuperType(other: TolkType): Boolean {
        if (other == TolkType.Never) return true
        if (other !is TolkIntType) return false
        return range.contains(other.range)
    }

    override fun join(other: TolkType): TolkType {
        if (other == this) return this
        if (other == TolkType.Never) return this
        if (other !is TolkIntType) return TolkUnionType.create(this, other)
        val range = this.range.join(other.range)
        if (range is TvmIntRangeSet.Point) return TolkConstantIntType(range.value)
        return TolkIntRangeType(range)
    }
}

data class TolkConstantIntType(
    override val value: BigInteger
) : TolkIntType, TolkConstantType<BigInteger> {
    constructor(value: Long) : this(value.toBigInteger())

    override val range: TvmIntRangeSet get() = TvmIntRangeSet.point(value)

    override fun negate(): TolkIntType = TolkConstantIntType(value.negate())

    override fun isSuperType(other: TolkType): Boolean = other == this || other == TolkNeverType

    override fun toString(): String = if (value.bitLength() <= 16) value.toString() else "int"
}

data class TolkIntRangeType(
    override val range: TvmIntRangeSet
) : TolkIntType {

    override fun negate(): TolkIntType = TolkIntRangeType(range.unaryMinus())

    override fun toString(): String = "int"
}
