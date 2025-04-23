package org.ton.intellij.tolk.type

import org.ton.intellij.tolk.psi.TolkStruct

data class TolkStructType(
    val psi: TolkStruct
) : TolkType {
    override fun join(other: TolkType): TolkType {
        if (other.unwrapTypeAlias() == this) return other
        return TolkUnionType.create(this, other)
    }

    override fun printDisplayName(appendable: Appendable): Appendable {
        return appendable.append(psi.name)
    }

    override fun toString(): String {
        return "TolkStructType(${psi.name}@${psi.containingFile?.name})"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TolkStructType) return false
        return psi.manager.areElementsEquivalent(psi,other.psi)
    }
}
