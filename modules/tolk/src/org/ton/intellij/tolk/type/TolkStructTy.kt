package org.ton.intellij.tolk.type

import com.intellij.codeInsight.completion.CompletionUtil
import org.ton.intellij.tolk.psi.TolkStruct
import org.ton.intellij.tolk.psi.TolkTypeExpression

data class TolkStructTy private constructor(
    val psi: TolkStruct,
    val typeArguments: List<TolkTy> = emptyList(),
) : TolkTy {
    private val hasGenerics = typeArguments.any { it.hasGenerics() }

    override fun hasGenerics(): Boolean = hasGenerics

    override fun join(other: TolkTy): TolkTy {
        if (other.unwrapTypeAlias() == this) return other
        return TolkUnionTy.create(this, other)
    }

    override fun superFoldWith(folder: TypeFolder): TolkTy {
        val newTypeArguments = typeArguments.map {
            it.foldWith(folder)
        }
        return TolkStructTy(psi, newTypeArguments)
    }

    override fun isEquivalentToInner(other: TolkTy): Boolean {
        if (this === other) return true
        if (other !is TolkStructTy) return false
        if (!psi.manager.areElementsEquivalent(psi,other.psi)) return false
//        if (typeArguments.size != other.typeArguments.size) return false
//        for (i in typeArguments.indices) {
//            if (!typeArguments[i].unwrapTypeAlias().isEquivalentTo(other.typeArguments[i])) return false
//        }
        return true
    }

    companion object {
        fun create(struct: TolkStruct, args: List<TolkTypeExpression>? = null): TolkStructTy {
            val typeParameters = mutableListOf<TolkTy>()
            struct.typeParameterList?.typeParameterList?.forEachIndexed { index, param ->
                typeParameters += args?.getOrNull(index)?.type ?: TolkTypeParameterTy.create(param)
            }
            return TolkStructTy(CompletionUtil.getOriginalOrSelf(struct), typeParameters)
        }
    }
}
