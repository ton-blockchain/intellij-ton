package org.ton.intellij.tolk.type

import org.ton.intellij.tolk.psi.TolkTypeDef
import org.ton.intellij.util.recursionGuard

class TolkTyAlias private constructor(
    val psi: TolkTypeDef,
    val underlyingType: TolkTy
) : TolkTy {
    override val hasTypeAlias: Boolean = true
    private var hashCode: Int = 0

    override fun canRhsBeAssigned(other: TolkTy): Boolean {
        if (other == this) return true
        return underlyingType.canRhsBeAssigned(other)
    }

    override fun toString(): String = "TolkAliasType($underlyingType)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TolkTyAlias) return false
        if (hashCode != 0 && other.hashCode != 0 && hashCode != other.hashCode) return false
        return psi == other.psi && underlyingType == other.underlyingType
    }

    override fun hashCode(): Int {
        var result = hashCode
        if (result == 0) {
            result = psi.hashCode()
            result = 31 * result + underlyingType.hashCode()
            hashCode = result
        }
        return result
    }

    companion object {
        fun create(psi: TolkTypeDef): TolkTy {
            val underlyingType = recursionGuard(psi) {
                psi.typeExpression?.type
            } ?: TolkTy.Unknown
            return when(underlyingType) {
                TolkTyNull,
                TolkTyNever,
                TolkTyVoid -> underlyingType // aliasing these types is strange, don't store an alias
                else ->  TolkTyAlias(psi, underlyingType)
            }
        }
    }


}
