package org.ton.intellij.tolk.type

import org.ton.intellij.tolk.psi.TolkTypeDef
import org.ton.intellij.tolk.psi.TolkTypeParameter

class TolkAliasType(
    val psi: TolkTypeDef,
    val underlyingType: TolkType
) : TolkType by underlyingType {
    override fun printDisplayName(appendable: Appendable): Appendable {
        return psi.name?.let { appendable.append(it) } ?: underlyingType.printDisplayName(appendable)
    }

    override fun substitute(substitution: Map<TolkTypeParameter, TolkType>): TolkType {
        return this
    }

    override fun canRhsBeAssigned(other: TolkType): Boolean {
        if (other == this) return true
        return underlyingType.canRhsBeAssigned(other)
    }

    override fun unwrapTypeAlias(): TolkType {
        return underlyingType.unwrapTypeAlias()
    }

    override fun join(other: TolkType): TolkType {
        if (this == other) return this
        return TolkUnionType.create(this, other)
    }

    override fun toString(): String = "TolkAliasType($underlyingType)"
}
