package org.ton.intellij.tolk.type

import org.ton.intellij.tolk.type.range.TvmIntRangeSet
import java.math.BigInteger

interface TolkIntType : TolkPrimitiveType {
    val range: TvmIntRangeSet

    fun negate(): TolkIntType

    override fun actualType(): TolkType = TolkType.Int

    override fun isSuperType(other: TolkType): Boolean {
        if (other == TolkType.Never) return true
        if (other is TolkAliasType) return isSuperType(other.typeExpression)
        if (other !is TolkIntType) return false
        return range.contains(other.range)
    }

    override fun join(other: TolkType): TolkType {
        if (other == this) return this
        if (other == TolkType.Never) return this
        if (other is TolkAliasType) return join(other.typeExpression)
        if (other !is TolkIntType) return TolkUnionType.create(this, other)
        val range = this.range.join(other.range)
        if (range is TvmIntRangeSet.Point) return TolkConstantIntType(range.value)
        return TolkIntRangeType(range)
    }

    override fun printDisplayName(appendable: Appendable) = appendable.append("int")

}

data class TolkConstantIntType(
    override val value: BigInteger
) : TolkIntType, TolkConstantType<BigInteger> {
    constructor(value: Long) : this(value.toBigInteger())

    override val range: TvmIntRangeSet get() = TvmIntRangeSet.point(value)

    override fun negate(): TolkIntType = TolkConstantIntType(value.negate())

    override fun isSuperType(other: TolkType): Boolean {
        if (other is TolkAliasType) return isSuperType(other.typeExpression)
        return other == this || other == TolkNeverType
    }

    override fun toString(): String {
        val bitLength = value.bitLength()
        if (bitLength <= 16) return value.toString()
        return "0x${value.toString(16)}"
    }
}

data class TolkIntRangeType(
    override val range: TvmIntRangeSet
) : TolkIntType {

    override fun negate(): TolkIntType = TolkIntRangeType(range.unaryMinus())

    override fun toString(): String = range.toString()
}
