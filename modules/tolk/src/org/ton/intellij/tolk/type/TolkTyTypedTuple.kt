package org.ton.intellij.tolk.type

/**
 * `[int, slice]` is [TolkTyTypedTuple], a TVM 'tuple' under the hood, contained in 1 stack slot.
 * Unlike [TolkTyTuple] (untyped tuples), it has a predefined inner structure and can be assigned as
 * `var [i, cs] = [0, ""]`  (where i and cs become two separate variables on a stack, int and slice).
 */
class TolkTyTypedTuple private constructor(
    val elements: List<TolkTy>,
    private val hasGenerics: Boolean = elements.any { it.hasGenerics() },
    override val hasTypeAlias: Boolean = elements.any { it.hasTypeAlias }
) : TolkTy {
    private var hashCode: Int = 0

    override fun hasGenerics(): Boolean = hasGenerics

    override fun actualType(): TolkTyTypedTuple = TolkTyTypedTuple(
        elements.map { it.actualType() },
        hasGenerics
    )

    override fun superFoldWith(folder: TypeFolder): TolkTy {
        return create(
            elements.map { it.foldWith(folder) },
        )
    }

    override fun canRhsBeAssigned(other: TolkTy): Boolean {
        if (this == other) return true
        if (other is TolkTyAlias) return canRhsBeAssigned(other.underlyingType)
        if (other !is TolkTyTypedTuple) return other == TolkTy.Never
        if (elements.size != other.elements.size) return false
        for (i in elements.indices) {
            if (!elements[i].canRhsBeAssigned(other.elements[i])) return false
        }
        return true
    }

    override fun toString(): String = "[${elements.joinToString()}]"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TolkTyTypedTuple) return false

        if (hasGenerics != other.hasGenerics) return false
        if (hasTypeAlias != other.hasTypeAlias) return false
        if (elements.size != other.elements.size) return false
        if (hashCode != 0 && other.hashCode != 0 && hashCode != other.hashCode) return false
        return elements == other.elements
    }

    override fun hashCode(): Int {
        var result = hashCode
        if (result == 0) {
            result = 1 + elements.hashCode()
            result = 31 * result + hasTypeAlias.hashCode()
            result = 31 * result + hasGenerics.hashCode()
            hashCode = result
        }
        return result
    }

    companion object {
        fun create(vararg elements: TolkTy): TolkTy {
            return create(elements.toList())
        }

        fun create(elements: List<TolkTy>): TolkTy {
            return TolkTyTypedTuple(elements)
        }
    }
}
