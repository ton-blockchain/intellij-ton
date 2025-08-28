package org.ton.intellij.tolk.type

import com.intellij.codeInsight.completion.CompletionUtil
import org.ton.intellij.tolk.psi.TolkEnum

data class TolkTyEnum private constructor(
    override val psi: TolkEnum,
) : TolkTy, TolkTyPsiHolder {
    override val hasTypeAlias: Boolean get() = false

    override fun hasGenerics(): Boolean = false

    override fun superFoldWith(folder: TypeFolder): TolkTy {
        return TolkTyEnum(psi)
    }

    override fun canRhsBeAssigned(other: TolkTy): Boolean {
        if (this == other) return true
        if (other is TolkTyNever) return true
        if (other is TolkTyAlias) return canRhsBeAssigned(other.underlyingType)
        if (other is TolkTyEnum) {
            return psi.manager.areElementsEquivalent(psi, other.psi)
        }
        return isEquivalentTo(other)
    }

    override fun isEquivalentToInner(other: TolkTy): Boolean {
        if (this == other) return true
        if (other !is TolkTyEnum) return false
        return psi.manager.areElementsEquivalent(psi, other.psi)
    }

    companion object {
        fun create(enum: TolkEnum): TolkTyEnum {
            return TolkTyEnum(CompletionUtil.getOriginalOrSelf(enum))
        }
    }
}
