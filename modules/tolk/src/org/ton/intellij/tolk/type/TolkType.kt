package org.ton.intellij.tolk.type


import org.ton.intellij.tolk.psi.TolkTypeParameter
import org.ton.intellij.tolk.type.range.TvmIntRangeSet

sealed interface TolkType {
    fun nullable(): TolkType {
        return TolkUnionType.create(this, Null)
    }

    fun isNullable(): Boolean {
        return this is TolkUnionType && elements.contains(Null)
    }

    fun removeNullability(): TolkType = this

    /**
     * if A.isSuperType(B) then A.join(B) is A and A.meet(B) is B.
     */
    fun isSuperType(other: TolkType): Boolean = other == this || other == TolkNeverType

    fun join(other: TolkType): TolkType

    fun meet(other: TolkType): TolkType = if (other.isSuperType(this)) this else TolkNeverType

    fun visit(visitor: TolkTypeVisitor) {}

    fun substitute(substitution: Map<TolkTypeParameter, TolkType>): TolkType {
        return when (this) {
            is TolkTensorType -> tensor(elements.map { it.substitute(substitution) })
            is TolkTypedTupleType -> TolkTypedTupleType(elements.map { it.substitute(substitution) })
            is TolkUnionType -> TolkUnionType.create(elements.map { it.substitute(substitution) })
            is ParameterType -> substitution[this.psiElement] ?: this
            else -> this
        }
    }

    data class ParameterType(
        val psiElement: TolkTypeParameter
    ) : TolkType {
        val name: String get() = psiElement.name.toString()

        override fun toString(): String = name

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

        fun bool(value: Boolean): TolkType = if (value) TRUE else FALSE

        fun union(vararg elements: TolkType): TolkType {
            return TolkUnionType.create(elements.toList())
        }

        fun union(elements: Iterable<TolkType>): TolkType = TolkUnionType.create(elements.toList())

        fun tensor(elements: Collection<TolkType>): TolkType = TolkTensorType.create(elements.toList())

        fun typedTuple(elements: Collection<TolkType>): TolkType = TolkTypedTupleType(elements.toList())
    }
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

    override fun toString(): String = "never"
}

interface TolkTypeVisitor {
    fun visitTypeParameter(value: TolkType.ParameterType)
}
