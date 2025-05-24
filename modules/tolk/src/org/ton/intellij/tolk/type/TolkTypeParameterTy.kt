package org.ton.intellij.tolk.type

import com.intellij.codeInsight.completion.CompletionUtil
import org.ton.intellij.tolk.psi.TolkElement
import org.ton.intellij.tolk.psi.TolkReferenceTypeExpression
import org.ton.intellij.tolk.psi.TolkTypeParameter

class TolkTypeParameterTy private constructor(
    val parameter: TypeParameter
) : TolkTy {
    val name: String? get() = parameter.name

    override fun toString(): String = name.toString()

    override fun hasGenerics(): Boolean = true

    override fun join(other: TolkTy): TolkTy {
        if (this == other) return this
        return TolkUnionTy.create(this, other)
    }

    override fun equals(other: Any?): Boolean = other is TolkTypeParameterTy && other.parameter == this.parameter

    override fun hashCode(): Int = parameter.hashCode()

    sealed class TypeParameter {
        abstract val psi: TolkElement
        abstract val name: String?
    }

    data class ReceiverTypeParameter(
        override val psi: TolkReferenceTypeExpression
    ) : TypeParameter() {
        override val name: String get() = psi.identifier.text.removeSurrounding("`")

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as ReceiverTypeParameter
            return psi.manager.areElementsEquivalent(psi, other.psi)
        }

        override fun hashCode(): Int = psi.hashCode()
    }

    data class NamedTypeParameter(
        override val psi: TolkTypeParameter
    ) : TypeParameter() {
        override val name: String? get() = psi.name

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as NamedTypeParameter
            return psi.manager.areElementsEquivalent(psi, other.psi)
        }

        override fun hashCode(): Int = psi.hashCode()
    }

    companion object {
        fun create(psi: TolkTypeParameter): TolkTypeParameterTy {
            val original = CompletionUtil.getOriginalOrSelf(psi)
            return TolkTypeParameterTy(
                NamedTypeParameter(original)
            )
        }

        fun create(psi: TolkReferenceTypeExpression): TolkTypeParameterTy {
            val original = CompletionUtil.getOriginalOrSelf(psi)
            return TolkTypeParameterTy(
                ReceiverTypeParameter(original)
            )
        }
    }
}
