package org.ton.intellij.tolk.type

/**
 * `(int, slice)` is [TolkTyTensor] of 2 elements. Tensor of N elements occupies N stack slots.
 * Of course, there may be nested tensors, like `(int, (int, slice), cell)`.
 * Arguments, variables, globals, return values, etc. can be tensors.
 * A tensor can be empty.
 */
class TolkTyTensor private constructor(
    val elements: List<TolkTy>,
    val hasGenerics: Boolean = elements.any { it.hasGenerics() },
    override val hasTypeAlias: Boolean = elements.any { it.hasTypeAlias }
) : TolkTy {
    private var hashCode: Int = 0

    override fun toString(): String = "(${elements.joinToString()})"

    override fun hasGenerics(): Boolean = hasGenerics

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TolkTyTensor) return false
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

    override fun actualType(): TolkTyTensor = TolkTyTensor(
        elements.map { it.actualType() },
        hasGenerics
    )

    override fun canRhsBeAssigned(other: TolkTy): Boolean {
        if (this == other) return true
        if (other is TolkTyAlias) return canRhsBeAssigned(other.underlyingType)
        if (other !is TolkTyTensor) return other == TolkTy.Never
        if (elements.size != other.elements.size) return false
        for (i in elements.indices) {
            val element = elements[i]
            val otherElement = other.elements[i]
            if (!element.canRhsBeAssigned(otherElement)) {
                return false
            }
        }
        return true
    }

    companion object {
        val EMPTY = TolkTyTensor(emptyList())

        fun create(vararg elements: TolkTy): TolkTy {
            return create(elements.toList())
        }

        fun create(elements: List<TolkTy>): TolkTy {
            return when(elements.size) {
                0 -> EMPTY
                1 -> elements[0]
                else -> TolkTyTensor(elements)
            }
        }
    }
}
