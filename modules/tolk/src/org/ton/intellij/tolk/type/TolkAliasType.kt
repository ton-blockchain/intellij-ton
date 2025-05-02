package org.ton.intellij.tolk.type

import org.ton.intellij.tolk.psi.TolkElement
import org.ton.intellij.tolk.psi.TolkTypeDef

class TolkAliasType(
    val psi: TolkTypeDef,
    val underlyingType: TolkType
) : TolkType by underlyingType {
    override fun renderAppendable(appendable: Appendable): Appendable {
        return psi.name?.let { appendable.append(it) } ?: underlyingType.renderAppendable(appendable)
    }

    override fun substitute(substitution: Map<TolkElement, TolkType>): TolkType {
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
