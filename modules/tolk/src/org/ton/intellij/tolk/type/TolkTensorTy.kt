package org.ton.intellij.tolk.type

data class TolkTensorTy private constructor(
    val elements: List<TolkTy>,
    val hasGenerics: Boolean
) : TolkTy {
    private var hashCode: Int = 0

    override fun toString(): String = "(${elements.joinToString()})"

    override fun hasGenerics(): Boolean = hasGenerics

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TolkTensorTy) return false
        if (hasGenerics != other.hasGenerics) return false
        if (elements.size != other.elements.size) return false
        if (hashCode != 0 && other.hashCode != 0 && hashCode != other.hashCode) return false
        return elements == other.elements
    }

    override fun hashCode(): Int {
        var result = hashCode
        if (result == 0) {
            result = elements.hashCode() * 31 + hasGenerics.hashCode()
            hashCode = result
        }
        return result
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
        return TolkUnionTy.create(this, other)
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
        if (other is TolkTypeAliasTy) return canRhsBeAssigned(other.unwrapTypeAlias())
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
