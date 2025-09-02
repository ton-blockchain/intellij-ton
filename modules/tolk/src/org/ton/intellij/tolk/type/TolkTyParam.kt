package org.ton.intellij.tolk.type

import com.intellij.codeInsight.completion.CompletionUtil
import org.ton.intellij.tolk.psi.TolkElement
import org.ton.intellij.tolk.psi.TolkReferenceTypeExpression
import org.ton.intellij.tolk.psi.TolkTypeParameter

class TolkTyParam private constructor(
    val parameter: TypeParameter,
) : TolkTy, TolkTyPsiHolder {
    val name: String? get() = parameter.name

    override val hasTypeAlias: Boolean get() = false

    override fun toString(): String = name.toString()

    override fun hasGenerics(): Boolean = true

    override fun equals(other: Any?): Boolean = other is TolkTyParam && other.parameter == this.parameter

    override fun isEquivalentToInner(other: TolkTy): Boolean {
        if (other == this) return true
        if (other !is TolkTyParam) return false
        return other.name == name
    }

    override fun canRhsBeAssigned(other: TolkTy): Boolean = true

    override fun hashCode(): Int = parameter.hashCode()

    override val psi: TolkElement
        get() = parameter.psi

    sealed class TypeParameter {
        abstract val psi: TolkElement
        abstract val name: String?
    }

    data class ReceiverTypeParameter(
        override val psi: TolkReferenceTypeExpression
    ) : TypeParameter() {
        override val name: String get() = psi.referenceName ?: psi.identifier.text.removeSurrounding("`")

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
        fun create(psi: TolkTypeParameter): TolkTyParam {
            val original = CompletionUtil.getOriginalOrSelf(psi)
            return TolkTyParam(
                NamedTypeParameter(original)
            )
        }

        fun create(psi: TolkReferenceTypeExpression): TolkTyParam {
            val original = CompletionUtil.getOriginalOrSelf(psi)
            return TolkTyParam(
                ReceiverTypeParameter(original)
            )
        }
    }
}
