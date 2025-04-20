package org.ton.intellij.tolk.type


import org.ton.intellij.tolk.psi.TolkStruct
import org.ton.intellij.tolk.psi.TolkTypeDef
import org.ton.intellij.tolk.psi.TolkTypeParameter
import org.ton.intellij.tolk.type.range.TvmIntRangeSet

sealed interface TolkType {
    fun nullable(): TolkType {
        return TolkUnionType.create(this, Null)
    }

    fun isNullable(): Boolean {
        return this is TolkUnionType && variants.contains(Null)
    }

    fun removeNullability(): TolkType = this

    fun actualType(): TolkType = unwrapTypeAlias()

    fun unwrapTypeAlias(): TolkType = this

    /**
     * if A.isSuperType(B) then A.join(B) is A and A.meet(B) is B.
     */
    fun isSuperType(other: TolkType): Boolean {
        if (other is TolkAliasType) return isSuperType(other.underlyingType)
        return other == this || other == TolkNeverType
    }

    fun join(other: TolkType): TolkType

    fun meet(other: TolkType): TolkType = if (other.isSuperType(this)) this else TolkNeverType

    fun visit(visitor: TolkTypeVisitor) {}

    fun hasGenerics(): Boolean = false

    fun substitute(substitution: Map<TolkTypeParameter, TolkType>): TolkType {
        return when (this) {
            is TolkTensorType -> tensor(elements.map { it.substitute(substitution) })
            is TolkTypedTupleType -> TolkTypedTupleType.create(elements.map { it.substitute(substitution) })
            is ParameterType -> substitution[this.psiElement] ?: this
            else -> this
        }
    }

    fun printDisplayName(appendable: Appendable): Appendable = appendable.append(toString())
    fun canRhsBeAssigned(other: TolkType): Boolean {
        if (other == this) return true
        if (other is TolkAliasType) return canRhsBeAssigned(other.unwrapTypeAlias())
        return other == Never
    }

    data class ParameterType(
        val psiElement: TolkTypeParameter
    ) : TolkType {
        val name: String get() = psiElement.name.toString()

        override fun toString(): String = name

        override fun hasGenerics(): Boolean = true

        override fun join(other: TolkType): TolkType {
            if (this == other) return this
            return TolkUnionType.create(this, other)
        }

        override fun visit(visitor: TolkTypeVisitor) {
            visitor.visitTypeParameter(this)
        }
    }

    companion object {
        val Int = TolkIntRangeType(TvmIntRangeSet.ALL)
        val TRUE = TolkConstantBoolType(true)
        val FALSE = TolkConstantBoolType(false)
        val Bool = TolkBoolType
        val Null = TolkNullType
        val Unit = TolkUnitType
        val Cell = TolkCellType
        val Slice = TolkSliceType
        val Builder = TolkBuilderType
        val Continuation = TolkContinuationType
        val Tuple = TolkTupleType
        val Unknown = TolkUnknownType
        val Never = TolkNeverType
        val Coins = TolkCoinsType()
        val VarInt32 = TolkVarInt32Type()
        val VarInt16 = TolkVarInt16Type()
        val Address = TolkAddressType

        // add to TolkTypeCompletionProvider also
        fun byName(text: String): TolkType? {
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

        fun bool(value: Boolean): TolkType = if (value) TRUE else FALSE

        fun union(vararg elements: TolkType): TolkType {
            return TolkUnionType.create(elements.toList())
        }

        fun union(elements: Iterable<TolkType>): TolkType = TolkUnionType.create(elements.toList())

        fun tensor(elements: List<TolkType>): TolkType = TolkTensorType.create(elements)

        fun typedTuple(elements: List<TolkType>): TolkType = TolkTypedTupleType.create(elements)

        fun uint(n: Int): TolkType = TolkIntNType(n, unsigned = true)

        fun int(n: Int): TolkType = TolkIntNType(n, unsigned = false)

        fun bits(n: Int): TolkType = TolkBitsNType(n)

        fun bytes(n: Int): TolkType = TolkBytesNType(n)

        fun alias(typeDef: TolkTypeDef, typeExpression: TolkType): TolkAliasType =
            TolkAliasType(typeDef, typeExpression)

        fun struct(struct: TolkStruct): TolkStructType = TolkStructType(struct)
    }
}

fun TolkType?.join(other: TolkType?): TolkType? {
    if (this == null || this == TolkType.Unknown) return other
    if (other == null || other == TolkType.Unknown) return this
    return this.join(other)
}

interface TolkPrimitiveType : TolkType

interface TolkConstantType<T> : TolkType {
    val value: T
}

object TolkUnitType : TolkPrimitiveType {
    override fun join(other: TolkType): TolkType {
        if (other == this) return this
        return TolkType.union(other, this)
    }

    override fun toString(): String = "()"
}

object TolkNullType : TolkPrimitiveType {
    override fun join(other: TolkType): TolkType {
        if (other == this) return this
        return TolkType.union(other, this)
    }

    override fun removeNullability(): TolkType = TolkType.Never

    override fun toString(): String = "null"
}

object TolkCellType : TolkPrimitiveType {
    override fun isSuperType(other: TolkType): Boolean = other == this

    override fun join(other: TolkType): TolkType {
        if (other is TolkCellType) return this
        return TolkType.union(this, other)
    }

    override fun meet(other: TolkType): TolkType {
        if (other is TolkCellType) return this
        return TolkNeverType
    }

    override fun toString(): String = "cell"
}

object TolkSliceType : TolkPrimitiveType {
    override fun isSuperType(other: TolkType): Boolean = other == this

    override fun join(other: TolkType): TolkType {
        if (other is TolkSliceType) return this
        return TolkType.union(this, other)
    }

    override fun meet(other: TolkType): TolkType {
        if (other is TolkSliceType) return this
        return TolkNeverType
    }

    override fun toString(): String = "slice"
}

object TolkBuilderType : TolkPrimitiveType {
    override fun isSuperType(other: TolkType): Boolean = other == this

    override fun join(other: TolkType): TolkType {
        if (other is TolkBuilderType) return this
        return TolkUnionType.create(this, other)
    }

    override fun meet(other: TolkType): TolkType {
        if (other is TolkBuilderType) return this
        return TolkNeverType
    }

    override fun toString(): String = "builder"
}

object TolkContinuationType : TolkPrimitiveType {
    override fun isSuperType(other: TolkType): Boolean = other == this

    override fun join(other: TolkType): TolkType {
        if (other is TolkContinuationType) return this
        return TolkType.union(this, other)
    }

    override fun meet(other: TolkType): TolkType {
        if (other is TolkContinuationType) return this
        return TolkNeverType
    }

    override fun toString(): String = "continuation"
}

object TolkTupleType : TolkPrimitiveType {
    override fun isSuperType(other: TolkType): Boolean = other == this

    override fun join(other: TolkType): TolkType {
        if (other is TolkTupleType) return this
        return TolkType.union(this, other)
    }

    override fun meet(other: TolkType): TolkType {
        if (other is TolkTupleType) return this
        return TolkNeverType
    }

    override fun toString(): String = "tuple"
}

object TolkUnknownType : TolkType {
    override fun isSuperType(other: TolkType): Boolean = true
    override fun join(other: TolkType): TolkType = this
    override fun meet(other: TolkType): TolkType = other

    override fun toString(): String = "unknown"
}

object TolkNeverType : TolkType {
    override fun isSuperType(other: TolkType): Boolean = other == this
    override fun join(other: TolkType): TolkType = other
    override fun meet(other: TolkType): TolkType = this

    override fun canRhsBeAssigned(other: TolkType): Boolean = true

    override fun toString(): String = "never"
}

object TolkAddressType : TolkPrimitiveType {
    override fun isSuperType(other: TolkType): Boolean = other == this

    override fun join(other: TolkType): TolkType {
        if (other is TolkAddressType) return this
        return TolkUnionType.create(this, other)
    }

    override fun meet(other: TolkType): TolkType {
        if (other is TolkAddressType) return this
        return TolkNeverType
    }

    override fun toString(): String = "address"
}

data class TolkCoinsType(
    override val range: TvmIntRangeSet = TvmIntRangeSet.ALL
) : TolkIntType {
    override fun negate(): TolkIntType = TolkCoinsType(range.unaryMinus())

    override fun printDisplayName(appendable: Appendable) = appendable.append("coins")

    override fun toString(): String = "coins"
}

data class TolkIntNType(
    val n: Int,
    val unsigned: Boolean,
    override val range: TvmIntRangeSet = TvmIntRangeSet.ALL
) : TolkIntType {
    override fun negate(): TolkIntType = TolkIntNType(n, unsigned,range.unaryMinus())

    override fun actualType(): TolkType = this

    override fun toString(): String = if (unsigned) {
        "uint$n"
    } else {
        "int$n"
    }

    override fun printDisplayName(appendable: Appendable) = appendable.append(toString())

    override fun canRhsBeAssigned(other: TolkType): Boolean {
        if (other == this) return true
        if (other.actualType() == TolkType.Int) return true
        if (other is TolkAliasType) return canRhsBeAssigned(other.unwrapTypeAlias())
        return other == TolkType.Never
    }
}

data class TolkBitsNType(
    val n: Int,
) : TolkType {
    override fun toString(): String = "bits$n"

    override fun actualType(): TolkType = this

    override fun join(other: TolkType): TolkType {
        if (this == other) return this
        return TolkUnionType.create(this, other)
    }
}

data class TolkBytesNType(
    val n: Int,
) : TolkType {
    override fun toString(): String = "bytes$n"

    override fun actualType(): TolkType = this

    override fun join(other: TolkType): TolkType {
        if (this == other) return this
        return TolkUnionType.create(this, other)
    }

    override fun canRhsBeAssigned(other: TolkType): Boolean {
        return other.unwrapTypeAlias() == this
    }
}

data class TolkVarInt32Type(
    override val range: TvmIntRangeSet = TvmIntRangeSet.ALL
) : TolkIntType {
    override fun negate(): TolkIntType = TolkVarInt32Type(range.unaryMinus())

    override fun actualType(): TolkType = this

    override fun printDisplayName(appendable: Appendable) = appendable.append("varint32")

    override fun toString(): String = "varint32"
}

data class TolkVarInt16Type(
    override val range: TvmIntRangeSet = TvmIntRangeSet.ALL
) : TolkIntType {
    override fun negate(): TolkIntType = TolkVarInt16Type(range.unaryMinus())

    override fun actualType(): TolkType = this

    override fun printDisplayName(appendable: Appendable) = appendable.append("varint16")

    override fun toString(): String = "varint16"
}

interface TolkTypeVisitor {
    fun visitTypeParameter(value: TolkType.ParameterType)
}
