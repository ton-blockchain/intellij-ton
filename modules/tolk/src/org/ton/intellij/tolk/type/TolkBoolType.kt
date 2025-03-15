package org.ton.intellij.tolk.type

interface TolkBoolType : TolkPrimitiveType {

    fun negate(): TolkBoolType = TolkType.Bool

    override fun printDisplayName(appendable: Appendable) {
        appendable.append("bool")
    }

    companion object : TolkBoolType {
        override fun isSuperType(other: TolkType): Boolean = other == TolkType.Never || other is TolkBoolType
        override fun join(other: TolkType): TolkType {
            if (other is TolkBoolType) return this
            return TolkType.union(this, other)
        }

        override fun toString(): String = "bool"
    }
}

data class TolkConstantBoolType(
    override val value: Boolean
) : TolkBoolType, TolkConstantType<Boolean> {

    override fun toString(): String = value.toString()

    override fun negate(): TolkBoolType {
        return if (value) TolkType.FALSE else TolkType.TRUE
    }

    override fun join(other: TolkType): TolkType {
        if (other == this) return this
        if (other is TolkBoolType) return TolkType.Bool
        return TolkType.union(this, other)
    }
}
