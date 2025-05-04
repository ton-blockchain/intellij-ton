package org.ton.intellij.tolk.type

abstract class TolkBoolTy : TolkPrimitiveTy {

    open fun negate(): TolkBoolTy = TolkTy.Bool

    override fun actualType(): TolkTy = TolkTy.Bool

    companion object : TolkBoolTy() {
        override fun isSuperType(other: TolkTy): Boolean = other == TolkTy.Never || other is TolkBoolTy
        override fun join(other: TolkTy): TolkTy {
            if (other is TolkBoolTy) return this
            return TolkTy.union(this, other)
        }

        override fun toString(): String = "bool"
    }
}

data class TolkConstantBoolTy(
    override val value: Boolean
) : TolkBoolTy(), TolkConstantTy<Boolean> {

    override fun toString(): String = value.toString()

    override fun negate(): TolkBoolTy {
        return if (value) TolkTy.FALSE else TolkTy.TRUE
    }

    override fun join(other: TolkTy): TolkTy {
        if (other == this) return this
        if (other is TolkBoolTy) return TolkTy.Bool
        return TolkTy.union(this, other)
    }
}
