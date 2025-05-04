package org.ton.intellij.tolk.type

data class TolkTensorTy private constructor(
    val elements: List<TolkTy>,
    val hasGenerics: Boolean
) : TolkTy {
    override fun toString(): String = "(${elements.joinToString()})"

    override fun renderAppendable(appendable: Appendable) = appendable.apply{
        append("(")
        var separator = ""
        elements.forEach {
            append(separator)
            it.renderAppendable(appendable)
            separator = ", "
        }
        append(")")
    }

    override fun superFoldWith(folder: TypeFolder): TolkTy {
        return create(
            elements.map { it.foldWith(folder) }
        )
    }

    override fun actualType(): TolkTensorTy = TolkTensorTy(
        elements.map { it.actualType() },
        hasGenerics
    )

    override fun join(other: TolkTy): TolkTy {
        if (this == other) return this
        if (other is TolkTensorTy && elements.size == other.elements.size) {
            var hasGenerics = false
            val newElements = elements.zip(other.elements).map { (a, b) ->
                val join = a.join(b)
                hasGenerics = hasGenerics || join.hasGenerics()
                join
            }
            return TolkTensorTy(newElements, hasGenerics)
        }
        return TyUnion.create(this, other)
    }

    override fun meet(other: TolkTy): TolkTy {
        if (this == other) return this
        if (other == TolkTy.Unknown) return this
        if (other is TolkTensorTy && elements.size == other.elements.size) {
            var hasGenerics = false
            val newElements = elements.zip(other.elements).map { (a, b) ->
                val meet = a.meet(b)
                hasGenerics = hasGenerics || meet.hasGenerics()
                meet
            }
            return TolkTensorTy(newElements, hasGenerics)
        }
        return TolkTy.Never
    }


    override fun canRhsBeAssigned(other: TolkTy): Boolean {
        if (this == other) return true
        if (other is TolkAliasTy) return canRhsBeAssigned(other.unwrapTypeAlias())
        if (other is TolkTensorTy && elements.size == other.elements.size) {
            return elements.zip(other.elements).all { (a, b) -> a.canRhsBeAssigned(b) }
        }
        return other == TolkTy.Never
    }

    companion object {
        fun create(vararg elements: TolkTy): TolkTy {
            return create(elements.toList())
        }

        fun create(elements: List<TolkTy>): TolkTy {
            if (elements.isEmpty()) return TolkUnitTy
            if (elements.size == 1) {
                return elements.single()
            }
            val hasGenerics = elements.any { it.hasGenerics() }
            return TolkTensorTy(elements, hasGenerics)
        }
    }
}
