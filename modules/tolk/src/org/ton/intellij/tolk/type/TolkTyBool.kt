package org.ton.intellij.tolk.type

abstract class TolkTyBool : TolkPrimitiveTy {

    open fun negate(): TolkTyBool = TolkTy.Bool

    override fun actualType(): TolkTy = TolkTy.Bool

    companion object : TolkTyBool() {
        override fun isSuperType(other: TolkTy): Boolean = other == TolkTy.Never || other is TolkTyBool
        override fun toString(): String = "bool"
    }

    override fun canRhsBeAssigned(other: TolkTy): Boolean {
        if (other is TolkTyBool) return true
        return super.canRhsBeAssigned(other)
    }
}

data class TolkConstantBoolTy(override val value: Boolean) :
    TolkTyBool(),
    TolkConstantTy<Boolean> {

    override fun toString(): String = value.toString()

    override fun negate(): TolkTyBool = if (value) TolkTy.FALSE else TolkTy.TRUE
}
