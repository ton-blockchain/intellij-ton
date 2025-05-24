package org.ton.intellij.tolk.type

data class TolkFunctionTy(
    val inputType: TolkTy,
    val returnType: TolkTy
) : TolkTy {
    override fun toString(): String = "$inputType -> $returnType"

    override fun hasGenerics(): Boolean {
        return inputType.hasGenerics() || returnType.hasGenerics()
    }

    val parameters: List<TolkTy> = if (inputType is TolkTensorTy) inputType.elements else listOf(inputType)

    override fun superFoldWith(folder: TypeFolder): TolkTy {
        return TolkFunctionTy(
            inputType.foldWith(folder),
            returnType.foldWith(folder)
        )
    }

    override fun join(other: TolkTy): TolkTy {
        if (this == other.unwrapTypeAlias()) return other
        if (other is TolkFunctionTy) {
            return TolkFunctionTy(inputType.join(other.inputType), returnType.join(other.returnType))
        }
        return TolkUnionTy.create(this, other)
    }

    override fun meet(other: TolkTy): TolkTy {
        if (this == other) return this
        if (other == TolkTy.Unknown) return this
        if (other is TolkFunctionTy) {
            return TolkFunctionTy(inputType.meet(other.inputType), returnType.meet(other.returnType))
        }
        return TolkTy.Never
    }


    override fun canRhsBeAssigned(other: TolkTy): Boolean {
        if (this == other) return true
        if (other is TolkTypeAliasTy) return canRhsBeAssigned(other.unwrapTypeAlias())
        if (other !is TolkFunctionTy) return other == TolkTy.Never
        return inputType.canRhsBeAssigned(other.inputType)
                && returnType.canRhsBeAssigned(other.returnType)
    }
}
