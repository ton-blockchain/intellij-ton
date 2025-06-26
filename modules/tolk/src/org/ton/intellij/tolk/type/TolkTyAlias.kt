package org.ton.intellij.tolk.type

import org.ton.intellij.tolk.psi.TolkTypeDef
import org.ton.intellij.tolk.psi.TolkTypeExpression
import org.ton.intellij.util.recursionGuard

class TolkTyAlias private constructor(
    val psi: TolkTypeDef,
    val underlyingType: TolkTy,
    val typeArguments: List<TolkTy> = emptyList(),
) : TolkTy {
    override val hasTypeAlias: Boolean = true

    private var hashCode: Int = 0

    private val hasGenerics = typeArguments.any { it.hasGenerics() }

    override fun hasGenerics(): Boolean = hasGenerics

    override fun canRhsBeAssigned(other: TolkTy): Boolean {
        if (other == this) return true
        return underlyingType.canRhsBeAssigned(other)
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
                    val newSubType = underlyingType.substitute(sub)

                    TolkTyAlias(psi, newSubType, args.orEmpty())
                }
            }
        }
    }
}
