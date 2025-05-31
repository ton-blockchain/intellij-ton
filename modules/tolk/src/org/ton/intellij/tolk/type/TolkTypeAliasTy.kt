package org.ton.intellij.tolk.type

import org.ton.intellij.tolk.psi.TolkTypeDef

data class TolkTypeAliasTy(
    val psi: TolkTypeDef,
    val underlyingType: TolkTy
) : TolkTy {
    override fun canRhsBeAssigned(other: TolkTy): Boolean {
        if (other == this) return true
        return underlyingType.canRhsBeAssigned(other)
    }

    override fun unwrapTypeAlias(): TolkTy {
        return underlyingType.unwrapTypeAlias()
    }

    override fun join(other: TolkTy): TolkTy {
        if (this == other) return this
        return TolkUnionTy.create(this, other)
    }

    override fun toString(): String = "TolkAliasType($underlyingType)"
}
