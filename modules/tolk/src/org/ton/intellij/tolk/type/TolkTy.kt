package org.ton.intellij.tolk.type


import org.ton.intellij.tolk.psi.TolkReferenceElement
import org.ton.intellij.tolk.psi.TolkStruct
import org.ton.intellij.tolk.type.TolkTy.Companion.Cell
import org.ton.intellij.tolk.type.TolkTy.Companion.Never
import org.ton.intellij.tolk.type.TolkTy.Companion.Slice
import org.ton.intellij.tolk.type.range.TvmIntRangeSet

interface TolkTy : TypeFoldable<TolkTy> {
    fun nullable(): TolkTy {
        return TolkUnionTy.create(this, Null)
    }

    fun removeNullable(): TolkTy {
        if (this is TolkUnionTy && variants.contains(Null)) {
            return subtract(Null)
        }
        return this
    }

    fun isNullable(): Boolean {
        return this is TolkUnionTy && variants.contains(Null)
    }

    fun actualType(): TolkTy = this

    fun unwrapTypeAlias(): TolkTy = this

    /**
     * if A.isSuperType(B) then A.join(B) is A and A.meet(B) is B.
     */
    fun isSuperType(other: TolkTy): Boolean {
        if (other is TolkTypeAliasTy) return isSuperType(other.underlyingType)
        return other == this || other == TolkTy.Never
    }

    fun join(other: TolkTy): TolkTy

    fun meet(other: TolkTy): TolkTy = if (other.isSuperType(this)) this else TolkNeverTy

    fun hasGenerics(): Boolean = false

    fun canRhsBeAssigned(other: TolkTy): Boolean {
        if (other == this) return true
        if (other is TolkTypeAliasTy) return canRhsBeAssigned(other.unwrapTypeAlias())
        return other == Never
    }

    fun isEquivalentTo(other: TolkTy?): Boolean = other != null && isEquivalentToInner(other.unwrapTypeAlias())

    fun isEquivalentToInner(other: TolkTy): Boolean = equals(other)

    override fun foldWith(folder: TypeFolder): TolkTy = folder.foldType(this)

    override fun superFoldWith(folder: TypeFolder): TolkTy = this

    companion object {
        val Int = TolkIntRangeTy(TvmIntRangeSet.ALL)
        val TRUE = TolkConstantBoolTy(true)
        val FALSE = TolkConstantBoolTy(false)
        val Bool = TolkBoolTy
        val Null = TolkNullTy
        val Void = TolkVoidTy
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
        fun byName(text: String) = TolkPrimitiveTy.fromName(text)

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

// return `T`, so that `T + subtract_type` = type
// example: `int?` - `null` = `int`
// example: `int | slice | builder | bool` - `bool | slice` = `int | builder`
// what for: `if (x != null)` / `if (x is T)`, to smart cast x inside if
fun TolkTy?.subtract(other: TolkTy?): TolkTy {
    val lhsUnion = this as? TolkUnionTy ?: return TolkNeverTy

    val restVariants = ArrayList<TolkTy>()
    if (other is TolkUnionTy) {
        if (lhsUnion.containsAll(other)) {
            for (lhsVariant in lhsUnion.variants) {
                if (!other.contains(lhsVariant)) {
                    restVariants.add(lhsVariant)
                }
            }
        }
    } else if (other != null && lhsUnion.contains(other)) {
        for (lhsVariant in lhsUnion.variants) {
            if (lhsVariant.actualType() != other.actualType()) {
                restVariants.add(lhsVariant)
            }
        }
    }
    if (restVariants.isEmpty()) {
        return TolkNeverTy
    }
    if (restVariants.size == 1) {
        return restVariants.first()
    }
    return TolkUnionTy.create(restVariants)
}

fun TolkTy?.isKnown(): Boolean {
    return !(this == null || this == TolkTy.Unknown)
}

fun TolkTy?.join(other: TolkTy?): TolkTy? {
    if (this == null || this == TolkTy.Unknown) return other
    if (other == null || other == TolkTy.Unknown) return this
    return this.join(other)
}

interface TolkPrimitiveTy : TolkTy {
    companion object {
        fun fromReference(element: TolkReferenceElement): TolkPrimitiveTy? {
            val name = element.referenceName ?: return null
            val result = fromName(name) ?: return null
            return result
        }

        fun fromName(text: String): TolkPrimitiveTy? {
            TolkIntNTy.fromName(text)?.let { return it }
            TolkBytesNTy.fromName(text)?.let { return it }
            TolkBitsNTy.fromName(text)?.let { return it }

            return when (text) {
                "int" -> TolkTy.Int
                "cell" -> Cell
                "slice" -> Slice
                "builder" -> TolkTy.Builder
                "continuation" -> TolkTy.Continuation
                "tuple" -> TolkTy.Tuple
                "void" -> TolkTy.Void
                "bool" -> TolkTy.Bool
                "never" -> TolkTy.Never
                "coins" -> TolkTy.Coins
                "varint16" -> TolkTy.VarInt16
                "varint32" -> TolkTy.VarInt32
                "address" -> TolkTy.Address
                else -> null
            }
        }
    }
}


interface TolkConstantTy<T> : TolkTy {
    val value: T
}

object TolkVoidTy : TolkPrimitiveTy {
    override fun join(other: TolkTy): TolkTy {
        if (other == this) return this
        return TolkTy.union(other, this)
    }

    override fun toString(): String = "void"
}

object TolkNullTy : TolkPrimitiveTy {
    override fun join(other: TolkTy): TolkTy {
        if (other == this) return this
        return TolkTy.union(other, this)
    }

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

object TolkNeverTy : TolkTy, TolkPrimitiveTy {
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
        return other == Never
    }

    companion object {
        val UINT8 = TolkIntNTy(8, unsigned = true)
        val UINT16 = TolkIntNTy(16, unsigned = true)
        val UINT32 = TolkIntNTy(32, unsigned = true)
        val UINT64 = TolkIntNTy(64, unsigned = true)
        val UINT128 = TolkIntNTy(128, unsigned = true)
        val UINT256 = TolkIntNTy(256, unsigned = true)

        val INT8 = TolkIntNTy(8, unsigned = false)
        val INT16 = TolkIntNTy(16, unsigned = false)
        val INT32 = TolkIntNTy(32, unsigned = false)
        val INT64 = TolkIntNTy(64, unsigned = false)
        val INT128 = TolkIntNTy(128, unsigned = false)
        val INT256 = TolkIntNTy(256, unsigned = false)

        val VALUES = listOf(
            UINT8, UINT16, UINT32, UINT64, UINT128, UINT256,
            INT8, INT16, INT32, INT64, INT128, INT256
        )

        fun uint(n: Int): TolkIntNTy = when (n) {
            8 -> UINT8
            16 -> UINT16
            32 -> UINT32
            64 -> UINT64
            128 -> UINT128
            256 -> UINT256
            else -> TolkIntNTy(n, unsigned = true)
        }

        fun int(n: Int): TolkIntNTy = when (n) {
            8 -> INT8
            16 -> INT16
            32 -> INT32
            64 -> INT64
            128 -> INT128
            256 -> INT256
            else -> TolkIntNTy(n, unsigned = false)
        }

        fun fromName(text: String): TolkIntNTy? {
            when (text) {
                "uint8" -> return UINT8
                "uint16" -> return UINT16
                "uint32" -> return UINT32
                "uint64" -> return UINT64
                "uint128" -> return UINT128
                "uint256" -> return UINT256
                "int8" -> return INT8
                "int16" -> return INT16
                "int32" -> return INT32
                "int64" -> return INT64
                "int128" -> return INT128
                "int256" -> return INT256
            }
            when {
                text.startsWith("uint") -> {
                    val n = text.removePrefix("uint").toIntOrNull() ?: return null
                    if (n in 1..1023) {
                        return TolkIntNTy(n, unsigned = true)
                    }
                    return null
                }

                text.startsWith("int") -> {
                    val n = text.removePrefix("int").toIntOrNull() ?: return null
                    if (n in 1..1023) {
                        return TolkIntNTy(n, unsigned = false)
                    }
                    return null
                }

                else -> return null
            }
        }
    }
}

data class TolkBitsNTy(
    val n: Int,
) : TolkPrimitiveTy {
    override fun toString(): String = "bits$n"

    override fun join(other: TolkTy): TolkTy {
        if (this == other) return this
        return TolkUnionTy.create(this, other)
    }

    companion object {
        fun fromName(text: String): TolkBitsNTy? {
            if (!text.startsWith("bits")) return null
            val n = text.removePrefix("bits").toIntOrNull() ?: return null
            if (n in 1..1024) {
                return TolkBitsNTy(n)
            }
            return null
        }
    }
}

data class TolkBytesNTy(
    val n: Int,
) : TolkPrimitiveTy {
    override fun toString(): String = "bytes$n"

    override fun join(other: TolkTy): TolkTy {
        if (this == other) return this
        return TolkUnionTy.create(this, other)
    }

    override fun canRhsBeAssigned(other: TolkTy): Boolean {
        return other.unwrapTypeAlias() == this
    }

    companion object {
        fun fromName(text: String): TolkBytesNTy? {
            if (!text.startsWith("bytes")) return null
            val n = text.removePrefix("bytes").toIntOrNull() ?: return null
            if (n in 1..1024) {
                return TolkBytesNTy(n)
            }
            return null
        }
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
