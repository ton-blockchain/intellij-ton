package org.ton.intellij.tolk.type

import org.ton.intellij.tolk.psi.TolkTypeParameter

data class TolkFunctionType(
    val inputType: TolkType,
    val returnType: TolkType
) : TolkType {
    override fun toString(): String = "$inputType -> $returnType"

    override fun printDisplayName(appendable: Appendable) {
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
    }

    override fun join(other: TolkType): TolkType {
        if (this == other) return this
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
        return TolkFunctionType(inputType.substitute(substitution), returnType.substitute(substitution))
    }

    fun resolveGenerics(functionCall: TolkFunctionType): TolkFunctionType {
        val fType = functionCall.meet(this)
        val mapping = HashMap<TolkTypeParameter, TolkType>()

        fun resolve(paramType: TolkType, argType: TolkType) {
            when {
                paramType is TolkFunctionType && argType is TolkFunctionType -> {
                    resolve(paramType.inputType, argType.inputType)
                    resolve(paramType.returnType, argType.returnType)
                }

                paramType is TolkTensorType && argType is TolkTensorType -> {
                    paramType.elements.zip(argType.elements).forEach { (a, b) -> resolve(a, b) }
                }

                paramType is TolkTypedTupleType && argType is TolkTypedTupleType -> {
                    paramType.elements.zip(argType.elements).forEach { (a, b) -> resolve(a, b) }
                }

                paramType is TolkUnionType && argType is TolkUnionType -> {
                    paramType.elements.zip(argType.elements).forEach { (a, b) -> resolve(a, b) }
                }

                paramType is TolkTypeParameter -> {
                    mapping[paramType] = argType
                }
            }
        }

        resolve(this, fType)
        return substitute(mapping)
    }
}
