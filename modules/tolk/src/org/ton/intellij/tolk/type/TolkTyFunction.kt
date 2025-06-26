package org.ton.intellij.tolk.type

/**
 * `fun(int, int) -> void` is [TolkTyFunction], think of is as a typed continuation.
 * A type of function `fun f(x: int) { return x; }` is actually `fun(int) -> int`.
 * So, when assigning it to a variable `var cb = f`, this variable also has this type.
 */
class TolkTyFunction(
    val parametersType: List<TolkTy>,
    val returnType: TolkTy,
    val hasGenerics: Boolean = parametersType.any { it.hasGenerics() } || returnType.hasGenerics(),
    override val hasTypeAlias: Boolean = parametersType.any { it.hasTypeAlias } || returnType.hasTypeAlias
) : TolkTy {
    private var hashCode: Int = 0

    override fun toString(): String = render()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TolkTyFunction) return false
        if (hasGenerics != other.hasGenerics) return false
        if (parametersType.size != other.parametersType.size) return false
        if (hashCode != 0 && other.hashCode != 0 && hashCode != other.hashCode) return false
        return returnType == other.returnType && parametersType == other.parametersType
    }

    override fun hashCode(): Int {
        var result = hashCode
        if (result == 0) {
            result = hasGenerics.hashCode()
            result = 31 * result + parametersType.hashCode()
            result = 31 * result + returnType.hashCode()
            hashCode = result
        }
        return result
    }

    override fun hasGenerics(): Boolean = hasGenerics

    override fun actualType(): TolkTy {
        return TolkTyFunction(
            parametersType = parametersType.map { it.actualType() },
            returnType = returnType.actualType()
        )
    }

    override fun superFoldWith(folder: TypeFolder): TolkTy {
        return TolkTyFunction(
            parametersType.map { it.foldWith(folder) },
            returnType.foldWith(folder)
        )
    }

    override fun join(other: TolkTy): TolkTy {
        if (this == other.unwrapTypeAlias()) return other
        if (other is TolkTyFunction) {
            var hasGenerics = false
            val newParameterType = parametersType.asSequence().zip(other.parametersType.asSequence()).map { (a,b) ->
                val join = a.join(b)
                hasGenerics = hasGenerics || join.hasGenerics()
                join
            }.toList()
            val newReturnType = returnType.join(other.returnType)
            hasGenerics = hasGenerics || newReturnType.hasGenerics()
            return TolkTyFunction(newParameterType, newReturnType, hasGenerics)
        }
        return TolkTyUnion.create(this, other)
    }

    override fun canRhsBeAssigned(other: TolkTy): Boolean {
        if (this == other) return true
        if (other is TolkTyAlias) return canRhsBeAssigned(other.unwrapTypeAlias())
        if (other !is TolkTyFunction) return other == TolkTy.Never
        if (parametersType.size != other.parametersType.size) return false
        for (i in parametersType.indices) {
            val param = parametersType[i]
            val otherParam = other.parametersType[i]
            if (!param.canRhsBeAssigned(otherParam)) {
                return false
            }
            if (!otherParam.canRhsBeAssigned(param)) {
                return false
            }
        }
        return returnType.canRhsBeAssigned(other.returnType) && other.returnType.canRhsBeAssigned(returnType)
    }
}
