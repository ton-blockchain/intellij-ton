package org.ton.intellij.tolk.type

import org.ton.intellij.tolk.psi.TolkElement
import org.ton.intellij.tolk.psi.TolkTypeDef

class TolkAliasTy(
    val psi: TolkTypeDef,
    val underlyingType: TolkTy
) : TolkTy {

    override fun substitute(substitution: Map<TolkElement, TolkTy>): TolkTy {
        return this
    }

    override fun canRhsBeAssigned(other: TolkTy): Boolean {
        if (other == this) return true
        return underlyingType.canRhsBeAssigned(other)
    }

    override fun unwrapTypeAlias(): TolkTy {
        return underlyingType.unwrapTypeAlias()
    }

    override fun join(other: TolkTy): TolkTy {
        if (this == other) return this
        return TyUnion.create(this, other)
    }

    override fun toString(): String = "TolkAliasType($underlyingType)"
}
