package org.ton.intellij.tolk.type


import org.ton.intellij.tolk.psi.TolkReferenceElement
import org.ton.intellij.tolk.psi.TolkStruct
import org.ton.intellij.tolk.type.TolkTy.Companion.Cell
import org.ton.intellij.tolk.type.TolkTy.Companion.Never
import org.ton.intellij.tolk.type.TolkTy.Companion.Slice
import org.ton.intellij.tolk.type.range.TvmIntRangeSet

interface TolkTy : TypeFoldable<TolkTy> {
    val hasTypeAlias: Boolean

    fun nullable(): TolkTy {
        return TolkTyUnion.create(this, Null)
    }

    fun removeNullable(): TolkTy {
        if (this is TolkTyUnion && variants.contains(Null)) {
            return subtract(Null)
        }
        return this
    }

    fun isNullable(): Boolean {
        return this is TolkTyUnion && variants.contains(Null)
    }

    fun actualType(): TolkTy = this

    /**
     * if A.isSuperType(B) then A.join(B) is A and A.meet(B) is B.
     */
    fun isSuperType(other: TolkTy): Boolean {
        if (other is TolkTyAlias) return isSuperType(other.underlyingType)
        return other == this || other == Never
    }

    fun join(other: TolkTy): TolkTy {
        if (this.isEquivalentTo(other)) return this
        if (other == Unknown) return Unknown
        if (other == Never) return this
        if (other == Null) return TolkTyUnion.create(this, other)

        val tensor1 = this as? TolkTyTensor
        val tensor2 = other as? TolkTyTensor
        if (tensor1 != null && tensor2 != null && tensor1.elements.size == tensor2.elements.size) {
            val types = ArrayList<TolkTy>(tensor1.elements.size)
            for (i in tensor1.elements.indices) {
                val type1 = tensor1.elements[i]
                val type2 = tensor2.elements[i]
                types.add(type1.join(type2))
            }
            return TolkTyTensor.create(types)
        }

        val tuple1 = this as? TolkTyTypedTuple
        val tuple2 = other as? TolkTyTypedTuple
        if (tuple1 != null && tuple2 != null && tuple1.elements.size == tuple2.elements.size) {
            val types = ArrayList<TolkTy>(tuple1.elements.size)
            for (i in tuple1.elements.indices) {
                val type1 = tuple1.elements[i]
                val type2 = tuple2.elements[i]
                types.add(type1.join(type2))
            }
            return TolkTyTypedTuple.create(types)
        }

        if (this is TolkTyAlias) return this.underlyingType.join(other)
        if (other is TolkTyAlias) return this.join(other.underlyingType)

        return TolkTyUnion.create(this, other)
    }

    fun meet(other: TolkTy): TolkTy = if (other.isSuperType(this)) this else TolkTyNever

    fun hasGenerics(): Boolean = false

    fun canRhsBeAssigned(other: TolkTy): Boolean {
        if (other == this) return true
        if (other is TolkTyAlias) return canRhsBeAssigned(other.unwrapTypeAlias())
        return other == Never
    }

    fun isEquivalentTo(other: TolkTy?): Boolean = other != null && isEquivalentToInner(other)

    fun isEquivalentToInner(other: TolkTy): Boolean {
        val actualType = this.actualType()
        val otherActualType = other.actualType()

        val unwrapped = actualType.unwrapTypeAliasDeeply()
        val otherUnwrapped = otherActualType.unwrapTypeAliasDeeply()
        return unwrapped == otherUnwrapped
    }

    fun unwrapTypeAlias(): TolkTy {
        var unwrapped = this
        while (unwrapped is TolkTyAlias) {
            unwrapped = unwrapped.underlyingType
        }
        return unwrapped
    }

    override fun foldWith(folder: TypeFolder): TolkTy = folder.foldType(this)

    override fun superFoldWith(folder: TypeFolder): TolkTy = this

    companion object {
        val Int = TolkIntRangeTy(TvmIntRangeSet.ALL)
        val TRUE = TolkConstantBoolTy(true)
        val FALSE = TolkConstantBoolTy(false)
        val Bool = TolkTyBool
        val Null = TolkTyNull
        val Void = TolkTyVoid
        val Cell = TolkCellTy
        val Slice = TolkSliceTy
        val Builder = TolkTyBuilder
        val Continuation = TolkTyContinuation
        val Tuple = TolkTyTuple
        val Unknown = TolkTyUnknown
        val Never = TolkTyNever
        val Coins = TolkTyCoins
        val VarInt32 = TolkTyVarInt32
        val VarInt16 = TolkTyVarInt16
        val VarUInt16 = TolkTyVarUInt16
        val VarUInt32 = TolkTyVarUInt32
        val Address = TolkTyAddress

        // add to TolkTypeCompletionProvider also
        fun byName(text: String) = TolkPrimitiveTy.fromName(text)

        fun bool(value: Boolean): TolkTy = if (value) TRUE else FALSE

        fun union(vararg elements: TolkTy): TolkTy {
            return TolkTyUnion.create(elements.toList())
        }

        fun union(elements: Iterable<TolkTy>): TolkTy = TolkTyUnion.create(elements.toList())

        fun tensor(elements: List<TolkTy>): TolkTy = TolkTyTensor.create(elements)

        fun typedTuple(elements: List<TolkTy>): TolkTy = TolkTyTypedTuple.create(elements)

        fun uint(n: Int): TolkTy = TolkIntNTy(n, unsigned = true)

        fun int(n: Int): TolkTy = TolkIntNTy(n, unsigned = false)

        fun bits(n: Int): TolkTy = TolkBitsNTy(n)

        fun bytes(n: Int): TolkTy = TolkBytesNTy(n)

        fun struct(struct: TolkStruct): TolkTyStruct = TolkTyStruct.create(struct)
    }
}

// return `T`, so that `T + subtract_type` = type
// example: `int?` - `null` = `int`
// example: `int | slice | builder | bool` - `bool | slice` = `int | builder`
// what for: `if (x != null)` / `if (x is T)`, to smart cast x inside if
fun TolkTy?.subtract(other: TolkTy?): TolkTy {
    val lhsUnion = this as? TolkTyUnion ?: return TolkTyNever

    val restVariants = ArrayList<TolkTy>()
    if (other is TolkTyUnion) {
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
        return TolkTyNever
    }
    if (restVariants.size == 1) {
        return restVariants.first()
    }
    return TolkTyUnion.create(restVariants)
}

fun TolkTy?.join(other: TolkTy?): TolkTy? {
    if (this == null || this == TolkTy.Unknown) return other
    if (other == null || other == TolkTy.Unknown) return this
    return this.join(other)
}

interface TolkPrimitiveTy : TolkTy {
    override val hasTypeAlias: Boolean get() = false

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
                "never" -> Never
                "coins" -> TolkTy.Coins
                "varint16" -> TolkTy.VarInt16
                "varuint16" -> TolkTy.VarUInt16
                "varint32" -> TolkTy.VarInt32
                "varuint32" -> TolkTy.VarUInt32
                "address" -> TolkTy.Address
                else -> null
            }
        }
    }
}


interface TolkConstantTy<T> : TolkTy {
    val value: T
}

object TolkTyVoid : TolkPrimitiveTy {
    override fun toString(): String = "void"
}

object TolkTyNull : TolkPrimitiveTy {
    override fun toString(): String = "TolkTy(null)"

    override fun join(other: TolkTy): TolkTy {
        if (other == this || other == Never) return this
        if (other == TolkTy.Unknown) return TolkTy.Unknown
        return TolkTyUnion.create(other, this)
    }
}

object TolkCellTy : TolkPrimitiveTy {
    override fun isSuperType(other: TolkTy): Boolean = other == this

    override fun toString(): String = "cell"
}

object TolkSliceTy : TolkPrimitiveTy {
    override fun isSuperType(other: TolkTy): Boolean = other == this

    override fun toString(): String = "slice"
}

object TolkTyBuilder : TolkPrimitiveTy {
    override fun isSuperType(other: TolkTy): Boolean = other == this

    override fun toString(): String = "builder"
}

object TolkTyContinuation : TolkPrimitiveTy {
    override fun isSuperType(other: TolkTy): Boolean = other == this

    override fun toString(): String = "continuation"
}

object TolkTyTuple : TolkPrimitiveTy {
    override fun isSuperType(other: TolkTy): Boolean = other == this

    override fun toString(): String = "tuple"
}

object TolkTyUnknown : TolkTy {
    override val hasTypeAlias: Boolean get() = false

    override fun isSuperType(other: TolkTy): Boolean = true
    override fun join(other: TolkTy): TolkTy = this

    override fun toString(): String = "unknown"
}

object TolkTyNever : TolkTy, TolkPrimitiveTy {
    override fun isSuperType(other: TolkTy): Boolean = other == this
    override fun join(other: TolkTy): TolkTy = other

    override fun canRhsBeAssigned(other: TolkTy): Boolean = other == this

    override fun toString(): String = "never"
}

object TolkTyAddress : TolkPrimitiveTy {
    override fun isSuperType(other: TolkTy): Boolean = other == this

    override fun toString(): String = "address"
}

object TolkTyCoins : TolkPrimitiveTy {
    override fun actualType(): TolkTy = this

    override fun toString(): String = "coins"
}

data class TolkIntNTy(
    val n: Int,
    val unsigned: Boolean,
) : TolkPrimitiveTy {
    override fun actualType(): TolkTy = this

    override fun toString(): String = if (unsigned) {
        "uint$n"
    } else {
        "int$n"
    }

    override fun canRhsBeAssigned(other: TolkTy): Boolean {
        if (other == this) return true
        if (other.actualType() == TolkTy.Int) return true
        if (other is TolkTyAlias) return canRhsBeAssigned(other.unwrapTypeAlias())
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
                    if (n in 1..256) {
                        return TolkIntNTy(n, unsigned = true)
                    }
                    return null
                }

                text.startsWith("int") -> {
                    val n = text.removePrefix("int").toIntOrNull() ?: return null
                    if (n in 1..257) {
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

    companion object {
        fun fromName(text: String): TolkBitsNTy? {
            if (!text.startsWith("bits")) return null
            val n = text.removePrefix("bits").toIntOrNull() ?: return null
            if (n in 1..1023) {
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

    override fun canRhsBeAssigned(other: TolkTy): Boolean {
        return other.unwrapTypeAlias() == this
    }

    companion object {
        fun fromName(text: String): TolkBytesNTy? {
            if (!text.startsWith("bytes")) return null
            val n = text.removePrefix("bytes").toIntOrNull() ?: return null
            if (n in 1..1023) {
                return TolkBytesNTy(n)
            }
            return null
        }
    }
}

 object TolkTyVarInt32 : TolkPrimitiveTy {
    override fun toString(): String = "varint32"
}

object TolkTyVarUInt32 : TolkPrimitiveTy {
    override fun toString(): String = "varuint32"
}

 object TolkTyVarInt16 : TolkPrimitiveTy {
    override fun toString(): String = "varint16"
}

object TolkTyVarUInt16 : TolkPrimitiveTy {
    override fun toString(): String = "varuint16"
}
