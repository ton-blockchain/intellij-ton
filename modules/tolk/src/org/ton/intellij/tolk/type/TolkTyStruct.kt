package org.ton.intellij.tolk.type

import com.intellij.codeInsight.completion.CompletionUtil
import org.ton.intellij.tolk.psi.TolkStruct
import org.ton.intellij.tolk.psi.TolkTypeExpression

data class TolkTyStruct private constructor(
    override val psi: TolkStruct,
    val typeArguments: List<TolkTy> = emptyList(),
) : TolkTy, TolkTyPsiHolder {
    override val hasTypeAlias: Boolean get() = typeArguments.any { it.hasTypeAlias }

    private val hasGenerics = typeArguments.any { it.hasGenerics() }

    override fun hasGenerics(): Boolean = hasGenerics

    override fun superFoldWith(folder: TypeFolder): TolkTy {
        val newTypeArguments = typeArguments.map {
            it.foldWith(folder)
        }
        return TolkTyStruct(psi, newTypeArguments)
    }

    override fun canRhsBeAssigned(other: TolkTy): Boolean {
        if (this == other) return true
        if (other is TolkTyNever) return true
        if (other is TolkTyAlias) return canRhsBeAssigned(other.underlyingType)
        if (other is TolkTyStruct) {
            if (typeArguments.size != other.typeArguments.size) return false
            if (!psi.manager.areElementsEquivalent(psi, other.psi)) return false

            // allow assigning `Foo<int>` to `Foo<T>`
            for (i in typeArguments.indices) {
                val typeArgument = typeArguments[i]
                if (!typeArgument.isEquivalentTo(other.typeArguments[i]) && typeArgument !is TolkTyParam) return false
            }
            return true
        }
        return isEquivalentTo(other)
    }

    override fun isEquivalentToInner(other: TolkTy): Boolean {
        if (this == other) return true
        if (other !is TolkTyStruct) return false
        if (typeArguments.size != other.typeArguments.size) return false
        if (!psi.manager.areElementsEquivalent(psi, other.psi)) return false
        for (i in typeArguments.indices) {
            if (!typeArguments[i].isEquivalentTo(other.typeArguments[i])) return false
        }
        return true
    }

    companion object {
        fun create(struct: TolkStruct, args: List<TolkTypeExpression>? = null): TolkTyStruct {
            val typeParameters = mutableListOf<TolkTy>()
            struct.typeParameterList?.typeParameterList?.forEachIndexed { index, param ->
                typeParameters += args?.getOrNull(index)?.type ?: TolkTyParam.create(param)
            }
            return TolkTyStruct(CompletionUtil.getOriginalOrSelf(struct), typeParameters)
        }
    }
}
