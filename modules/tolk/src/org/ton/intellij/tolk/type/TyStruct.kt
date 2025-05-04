package org.ton.intellij.tolk.type

import com.intellij.codeInsight.completion.CompletionUtil
import org.ton.intellij.tolk.psi.TolkStruct

data class TyStruct private constructor(
    val psi: TolkStruct,
    val typeArguments: List<TolkTy> = emptyList(),
) : TolkTy {
    override fun hasGenerics(): Boolean = typeArguments.any { it.hasGenerics() }

    override fun join(other: TolkTy): TolkTy {
        if (other.unwrapTypeAlias() == this) return other
        return TolkUnionTy.create(this, other)
    }

    override fun superFoldWith(folder: TypeFolder): TolkTy {
        return TyStruct(psi, typeArguments.map { it.foldWith(folder) })
    }

    override fun isEquivalentToInner(other: TolkTy): Boolean {
        if (this === other) return true
        if (other !is TyStruct) return false
        if (!psi.manager.areElementsEquivalent(psi,other.psi)) return false
        if (typeArguments.size != other.typeArguments.size) return false
        for (i in typeArguments.indices) {
            if (!typeArguments[i].isEquivalentTo(other.typeArguments[i])) return false
        }
        return true
    }

    companion object {
        fun create(struct: TolkStruct): TyStruct {
            val typeParameters = mutableListOf<TyTypeParameter>()
            struct.typeParameterList?.typeParameterList?.forEach { generic ->
                typeParameters += TyTypeParameter.create(generic)
            }
            return TyStruct(CompletionUtil.getOriginalOrSelf(struct), typeParameters)
        }
    }
}
