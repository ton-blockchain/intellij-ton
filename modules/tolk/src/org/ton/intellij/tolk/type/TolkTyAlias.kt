package org.ton.intellij.tolk.type

import org.ton.intellij.tolk.psi.TolkTypeDef
import org.ton.intellij.tolk.psi.TolkTypeExpression
import org.ton.intellij.util.recursionGuard

class TolkTyAlias private constructor(
    override val psi: TolkTypeDef,
    val underlyingType: TolkTy,
    val typeArguments: List<TolkTy> = emptyList(),
) : TolkTy, TolkTyPsiHolder {
    override val hasTypeAlias: Boolean = true

    private var hashCode: Int = 0

    private val hasGenerics = typeArguments.any { it.hasGenerics() }

    override fun hasGenerics(): Boolean = hasGenerics

    override fun canRhsBeAssigned(other: TolkTy): Boolean {
        if (other == this) return true
        return underlyingType.canRhsBeAssigned(other)
    }

    override fun isEquivalentToInner(other: TolkTy): Boolean {
        if (other is TolkTyAlias) {
            if (typeArguments.isNotEmpty() && other.typeArguments.isNotEmpty()) {
                if (typeArguments.size != other.typeArguments.size) return false
                for (i in 0..<minOf(typeArguments.size, other.typeArguments.size)) {
                    if (!typeArguments[i].isEquivalentTo(other.typeArguments[i])) return false
                }
            }

            if (psi.name == other.psi.name) return true

            // given `type UserId = int` and `type OwnerId = int`, treat them as NOT equal (they are also not assignable);
            // (but nevertheless, they will have the same type_id, and `UserId | OwnerId` is not a valid union)
            if (this.underlyingType.isEquivalentTo(other.underlyingType)) {
                return !areTwoEqualTypeAliasesDifferent(this, other)
            }
        }
        return underlyingType.isEquivalentTo(other)
    }

    override fun toString(): String = "TolkAliasType($underlyingType, typeArguments=$typeArguments)"

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

    override fun superFoldWith(folder: TypeFolder): TolkTy {
        val newUnderlyingType = underlyingType.foldWith(folder)
        val newTypeArguments = typeArguments.map {
            it.foldWith(folder)
        }
        return TolkTyAlias(psi, newUnderlyingType, newTypeArguments)
    }

    companion object {
        fun create(psi: TolkTypeDef, args: List<TolkTypeExpression>? = null): TolkTy {
            val underlyingType = recursionGuard(psi) {
                psi.typeExpression?.type
            } ?: TolkTy.Unknown
            return when (underlyingType) {
                TolkTyNull,
                TolkTyNever,
                TolkTyVoid -> underlyingType // aliasing these types is strange, don't store an alias
                else -> {
                    val params = psi.typeParameterList?.typeParameterList?.map {
                        TolkTyParam.create(it)
                    }
                    val args = args?.map { it.type ?: TolkTy.Unknown }
                    var sub = Substitution.empty()
                    if (params != null && args != null && params.size == args.size) {
                        for (i in params.indices) {
                            val param = params[i]
                            val arg = args[i]
                            sub = sub.deduce(param, arg)
                        }
                    }
//                    val newSubType = underlyingType.substitute(sub)

                    TolkTyAlias(psi, underlyingType, params.orEmpty()).substitute(sub)
                }
            }
        }
    }
}

// having `type UserId = int` and `type OwnerId = int` (when their underlying types are equal),
// make `UserId` and `OwnerId` NOT equal and NOT assignable (although they'll have the same type_id);
// it allows overloading methods for these types independently, e.g.
// > type BalanceList = dict
// > type AssetList = dict
// > fun BalanceList.validate(self)
// > fun AssetList.validate(self)
fun areTwoEqualTypeAliasesDifferent(
    t1: TolkTyAlias,
    t2: TolkTyAlias,
): Boolean {
    if (t1.psi.isEquivalentTo(t2.psi)) {
        return false
    }

    if (t1.typeArguments.isNotEmpty() && t2.typeArguments.isNotEmpty()) {
        return !t1.typeArguments.zip(t2.typeArguments).all { (l, r) -> l.isEquivalentTo(r) }
    }

    // handle `type MInt2 = MInt1`, as well as `type BalanceList = dict`, then they are equal
    val tUnd1 = t1.underlyingType as? TolkTyAlias
    val tUnd2 = t2.underlyingType as? TolkTyAlias
    val oneAliasesAnother = (tUnd1 != null && tUnd1.psi.isEquivalentTo(t2.psi)) || (tUnd2 != null && t1.psi.isEquivalentTo(tUnd2.psi))
    return !oneAliasesAnother
}
