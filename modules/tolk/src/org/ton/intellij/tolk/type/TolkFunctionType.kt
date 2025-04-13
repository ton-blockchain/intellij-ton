package org.ton.intellij.tolk.type

import org.ton.intellij.tolk.psi.TolkTypeParameter

data class TolkFunctionType(
    val inputType: TolkType,
    val returnType: TolkType
) : TolkType {
    override fun toString(): String = "$inputType -> $returnType"

    override fun hasGenerics(): Boolean {
        return inputType.hasGenerics() || returnType.hasGenerics()
    }

    val parameters: List<TolkType> = if (inputType is TolkTensorType) inputType.elements else listOf(inputType)

    override fun printDisplayName(appendable: Appendable): Appendable {
        when (inputType) {
            is TolkUnitType -> {
                appendable.append("()")
            }

            is TolkTensorType -> {
                inputType.printDisplayName(appendable)
            }

            else -> {
                appendable.append("(")
                inputType.printDisplayName(appendable)
                appendable.append(")")
            }
        }
        appendable.append(" -> ")
        returnType.printDisplayName(appendable)
        return appendable
    }

    override fun join(other: TolkType): TolkType {
        if (this == other.unwrapTypeAlias()) return other
        if (other is TolkFunctionType) {
            return TolkFunctionType(inputType.join(other.inputType), returnType.join(other.returnType))
        }
        return TolkUnionType.create(this, other)
    }

    override fun meet(other: TolkType): TolkType {
        if (this == other) return this
        if (other == TolkType.Unknown) return this
        if (other is TolkFunctionType) {
            return TolkFunctionType(inputType.meet(other.inputType), returnType.meet(other.returnType))
        }
        return TolkType.Never
    }

    override fun substitute(substitution: Map<TolkTypeParameter, TolkType>): TolkFunctionType {
        val inputType = inputType.substitute(substitution)
        val returnType = returnType.substitute(substitution)
        return TolkFunctionType(inputType, returnType)
    }

    override fun canRhsBeAssigned(other: TolkType): Boolean {
        if (this == other) return true
        if (other is TolkAliasType) return canRhsBeAssigned(other.unwrapTypeAlias())
        if (other !is TolkFunctionType) return other == TolkType.Never
        return inputType.canRhsBeAssigned(other.inputType)
                && returnType.canRhsBeAssigned(other.returnType)
    }
}
