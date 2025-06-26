package org.ton.intellij.tolk.type

abstract class TolkTyBool : TolkPrimitiveTy {

    open fun negate(): TolkTyBool = TolkTy.Bool

    override fun actualType(): TolkTy = TolkTy.Bool

    companion object : TolkTyBool() {
        override fun isSuperType(other: TolkTy): Boolean = other == TolkTy.Never || other is TolkTyBool
        override fun join(other: TolkTy): TolkTy {
            if (other.unwrapTypeAlias() is TolkTyBool) return this
            return super.join(other)
        }

        override fun toString(): String = "bool"
    }
}

data class TolkConstantBoolTy(
    override val value: Boolean
) : TolkTyBool(), TolkConstantTy<Boolean> {

    override fun toString(): String = value.toString()

    override fun negate(): TolkTyBool {
        return if (value) TolkTy.FALSE else TolkTy.TRUE
    }

    override fun join(other: TolkTy): TolkTy {
        if (other.unwrapTypeAlias() is TolkTyBool) return TolkTy.Bool
        return super<TolkTyBool>.join(other)
    }
}
