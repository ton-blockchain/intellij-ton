package org.ton.intellij.tolk.type


import org.ton.intellij.tolk.psi.TolkStruct
import org.ton.intellij.tolk.type.range.TvmIntRangeSet

interface TolkTy : TypeFoldable<TolkTy> {
    fun nullable(): TolkTy {
        return TolkUnionTy.create(this, Null)
    }

    fun isNullable(): Boolean {
        return this is TolkUnionTy && variants.contains(Null)
    }

    open fun removeNullability(): TolkTy = this

    open fun actualType(): TolkTy = this

    open fun unwrapTypeAlias(): TolkTy = this

    /**
     * if A.isSuperType(B) then A.join(B) is A and A.meet(B) is B.
     */
    open fun isSuperType(other: TolkTy): Boolean {
        if (other is TolkTypeAliasTy) return isSuperType(other.underlyingType)
        return other == this || other == TolkNeverTy
    }

    abstract fun join(other: TolkTy): TolkTy

    open fun meet(other: TolkTy): TolkTy = if (other.isSuperType(this)) this else TolkNeverTy

    open fun hasGenerics(): Boolean = false

    open fun canRhsBeAssigned(other: TolkTy): Boolean {
        if (other == this) return true
        if (other is TolkTypeAliasTy) return canRhsBeAssigned(other.unwrapTypeAlias())
        return other == Never
    }

    fun isEquivalentTo(other: TolkTy?): Boolean = other != null && isEquivalentToInner(other.unwrapTypeAlias())

    open fun isEquivalentToInner(other: TolkTy): Boolean = equals(other)

    override fun foldWith(folder: TypeFolder): TolkTy = folder.foldType(this)

    override fun superFoldWith(folder: TypeFolder): TolkTy = this

    companion object {
        val Int = TolkIntRangeTy(TvmIntRangeSet.ALL)
        val TRUE = TolkConstantBoolTy(true)
        val FALSE = TolkConstantBoolTy(false)
        val Bool = TolkBoolTy
        val Null = TolkNullTy
        val Unit = TolkUnitTy
        val Cell = TolkCellTy
        val Slice = TolkSliceTy
        val Builder = TolkBuilderTy
        val Continuation = TolkContinuationTy
        val Tuple = TolkTupleTy
        val Unknown = TolkUnknownTy
        val Never = TolkNeverTy
        val Coins = TolkCoinsTy()
        val VarInt32 = TolkVarInt32Ty()
        val VarInt16 = TolkVarInt16Ty()
        val Address = TolkAddressTy

        // add to TolkTypeCompletionProvider also
        fun byName(text: String): TolkTy? {
            return when (text) {
                "int" -> Int
                "cell" -> Cell
                "slice" -> Slice
                "builder" -> Builder
                "continuation" -> Continuation
                "tuple" -> Tuple
                "void" -> Unit
                "bool" -> Bool
                "never" -> Never
                "coins" -> Coins
                "varint16" -> VarInt16
                "varint32" -> VarInt32
                "address" -> Address
                else -> {
                    when {
                        text.startsWith("uint") -> {
                            val n = text.removePrefix("uint").toIntOrNull() ?: return null
                            if (n in 1..1023) {
                                return uint(n)
                            }
                            return null
                        }

                        text.startsWith("int") -> {
                            val n = text.removePrefix("int").toIntOrNull() ?: return null
                            if (n in 1..1023) {
                                return int(n)
                            }
                            return null
                        }

                        text.startsWith("bits") -> {
                            val n = text.removePrefix("bits").toIntOrNull() ?: return null
                            if (n in 1..1023) {
                                return bits(n)
                            }
                            return null
                        }

                        text.startsWith("bytes") -> {
                            val n = text.removePrefix("bytes").toIntOrNull() ?: return null
                            if (n in 1..128) {
                                return bytes(n)
                            }
                            return null
                        }

                        else -> null
                    }
                }
            }
        }

        fun bool(value: Boolean): TolkTy = if (value) TRUE else FALSE

        fun union(vararg elements: TolkTy): TolkTy {
            return TolkUnionTy.create(elements.toList())
        }

        fun union(elements: Iterable<TolkTy>): TolkTy = TolkUnionTy.create(elements.toList())

        fun tensor(elements: List<TolkTy>): TolkTy = TolkTensorTy.create(elements)

        fun typedTuple(elements: List<TolkTy>): TolkTy = TolkTypedTupleTy.create(elements)

        fun uint(n: Int): TolkTy = TolkIntNTy(n, unsigned = true)

        fun int(n: Int): TolkTy = TolkIntNTy(n, unsigned = false)

        fun bits(n: Int): TolkTy = TolkBitsNTy(n)

        fun bytes(n: Int): TolkTy = TolkBytesNTy(n)

        fun struct(struct: TolkStruct): TolkStructTy = TolkStructTy.create(struct)
    }
}

fun TolkTy?.isKnown(): Boolean {
    return !(this == null || this == TolkTy.Unknown)
}

fun TolkTy?.join(other: TolkTy?): TolkTy? {
    if (this == null || this == TolkTy.Unknown) return other
    if (other == null || other == TolkTy.Unknown) return this
    return this.join(other)
}

 interface TolkPrimitiveTy : TolkTy

interface TolkConstantTy<T> : TolkTy {
    abstract val value: T
}

object TolkUnitTy : TolkPrimitiveTy {
    override fun join(other: TolkTy): TolkTy {
        if (other == this) return this
        return TolkTy.union(other, this)
    }

    override fun toString(): String = "()"
}

object TolkNullTy : TolkPrimitiveTy {
    override fun join(other: TolkTy): TolkTy {
        if (other == this) return this
        return TolkTy.union(other, this)
    }

    override fun removeNullability(): TolkTy = TolkTy.Never

    override fun toString(): String = "null"
}

object TolkCellTy : TolkPrimitiveTy {
    override fun isSuperType(other: TolkTy): Boolean = other == this

    override fun join(other: TolkTy): TolkTy {
        if (other is TolkCellTy) return this
        return TolkTy.union(this, other)
    }

    override fun meet(other: TolkTy): TolkTy {
        if (other is TolkCellTy) return this
        return TolkNeverTy
    }

    override fun toString(): String = "cell"
}

object TolkSliceTy : TolkPrimitiveTy {
    override fun isSuperType(other: TolkTy): Boolean = other == this

    override fun join(other: TolkTy): TolkTy {
        if (other is TolkSliceTy) return this
        return TolkTy.union(this, other)
    }

    override fun meet(other: TolkTy): TolkTy {
        if (other is TolkSliceTy) return this
        return TolkNeverTy
    }

    override fun toString(): String = "slice"
}

object TolkBuilderTy : TolkPrimitiveTy {
    override fun isSuperType(other: TolkTy): Boolean = other == this

    override fun join(other: TolkTy): TolkTy {
        if (other is TolkBuilderTy) return this
        return TolkUnionTy.create(this, other)
    }

    override fun meet(other: TolkTy): TolkTy {
        if (other is TolkBuilderTy) return this
        return TolkNeverTy
    }

    override fun toString(): String = "builder"
}

object TolkContinuationTy : TolkPrimitiveTy {
    override fun isSuperType(other: TolkTy): Boolean = other == this

    override fun join(other: TolkTy): TolkTy {
        if (other is TolkContinuationTy) return this
        return TolkTy.union(this, other)
    }

    override fun meet(other: TolkTy): TolkTy {
        if (other is TolkContinuationTy) return this
        return TolkNeverTy
    }

    override fun toString(): String = "continuation"
}

object TolkTupleTy : TolkPrimitiveTy {
    override fun isSuperType(other: TolkTy): Boolean = other == this

    override fun join(other: TolkTy): TolkTy {
        if (other is TolkTupleTy) return this
        return TolkTy.union(this, other)
    }

    override fun meet(other: TolkTy): TolkTy {
        if (other is TolkTupleTy) return this
        return TolkNeverTy
    }

    override fun toString(): String = "tuple"
}

object TolkUnknownTy : TolkTy {
    override fun isSuperType(other: TolkTy): Boolean = true
    override fun join(other: TolkTy): TolkTy = this
    override fun meet(other: TolkTy): TolkTy = other

    override fun toString(): String = "unknown"
}

object TolkNeverTy : TolkTy {
    override fun isSuperType(other: TolkTy): Boolean = other == this
    override fun join(other: TolkTy): TolkTy = other
    override fun meet(other: TolkTy): TolkTy = this

    override fun canRhsBeAssigned(other: TolkTy): Boolean = true

    override fun toString(): String = "never"
}

object TolkAddressTy : TolkPrimitiveTy {
    override fun isSuperType(other: TolkTy): Boolean = other == this

    override fun join(other: TolkTy): TolkTy {
        if (other is TolkAddressTy) return this
        return TolkUnionTy.create(this, other)
    }

    override fun meet(other: TolkTy): TolkTy {
        if (other is TolkAddressTy) return this
        return TolkNeverTy
    }

    override fun toString(): String = "address"
}

data class TolkCoinsTy(
    override val range: TvmIntRangeSet = TvmIntRangeSet.ALL
) : TolkIntTy {
    override fun negate(): TolkIntTy = TolkCoinsTy(range.unaryMinus())

    override fun toString(): String = "coins"
}

data class TolkIntNTy(
    val n: Int,
    val unsigned: Boolean,
    override val range: TvmIntRangeSet = TvmIntRangeSet.ALL
) : TolkIntTy {
    override fun negate(): TolkIntTy = TolkIntNTy(n, unsigned, range.unaryMinus())

    override fun actualType(): TolkTy = this

    override fun toString(): String = if (unsigned) {
        "uint$n"
    } else {
        "int$n"
    }

    override fun canRhsBeAssigned(other: TolkTy): Boolean {
        if (other == this) return true
        if (other.actualType() == TolkTy.Int) return true
        if (other is TolkTypeAliasTy) return canRhsBeAssigned(other.unwrapTypeAlias())
        return other == TolkTy.Never
    }
}

data class TolkBitsNTy(
    val n: Int,
) : TolkTy {
    override fun toString(): String = "bits$n"

    override fun join(other: TolkTy): TolkTy {
        if (this == other) return this
        return TolkUnionTy.create(this, other)
    }
}

data class TolkBytesNTy(
    val n: Int,
) : TolkTy {
    override fun toString(): String = "bytes$n"

    override fun join(other: TolkTy): TolkTy {
        if (this == other) return this
        return TolkUnionTy.create(this, other)
    }

    override fun canRhsBeAssigned(other: TolkTy): Boolean {
        return other.unwrapTypeAlias() == this
    }
}

data class TolkVarInt32Ty(
    override val range: TvmIntRangeSet = TvmIntRangeSet.ALL
) : TolkIntTy {
    override fun negate(): TolkIntTy = TolkVarInt32Ty(range.unaryMinus())

    override fun toString(): String = "varint32"
}

data class TolkVarInt16Ty(
    override val range: TvmIntRangeSet = TvmIntRangeSet.ALL
) : TolkIntTy {
    override fun negate(): TolkIntTy = TolkVarInt16Ty(range.unaryMinus())

    override fun toString(): String = "varint16"
}
