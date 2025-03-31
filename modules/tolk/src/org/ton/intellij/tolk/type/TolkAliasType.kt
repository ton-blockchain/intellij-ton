package org.ton.intellij.tolk.type

import org.ton.intellij.tolk.psi.TolkTypeDef
import org.ton.intellij.tolk.psi.TolkTypeParameter

class TolkAliasType(
    val psi: TolkTypeDef,
    val typeExpression: TolkType
) : TolkType by typeExpression {
    override fun printDisplayName(appendable: Appendable): Appendable {
        return psi.name?.let { appendable.append(it) } ?: typeExpression.printDisplayName(appendable)
    }

    override fun substitute(substitution: Map<TolkTypeParameter, TolkType>): TolkType {
        return this
    }

    override fun toString(): String = "TolkAliasType($typeExpression)"
}
