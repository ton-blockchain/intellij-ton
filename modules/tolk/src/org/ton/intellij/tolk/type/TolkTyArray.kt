package org.ton.intellij.tolk.type

class TolkTyArray private constructor(
    val elementType: TolkTy,
    private val hasGenerics: Boolean = elementType.hasGenerics(),
    override val hasTypeAlias: Boolean = elementType.hasTypeAlias
) : TolkTy {
    private var hashCode: Int = 0

    override fun hasGenerics(): Boolean = hasGenerics

    override fun actualType(): TolkTyArray = TolkTyArray(
        elementType.actualType(),
        hasGenerics
    )

    override fun superFoldWith(folder: TypeFolder): TolkTy {
        return create(elementType.foldWith(folder))
    }

    override fun canRhsBeAssigned(other: TolkTy): Boolean {
        if (this == other) return true
        if (other is TolkTyAlias) return canRhsBeAssigned(other.underlyingType)
        if (other is TolkTyArray) {
            return elementType.canRhsBeAssigned(other.elementType)
        }
        if (other is TolkTyTypedTuple) {
            return other.elements.all {
                elementType.canRhsBeAssigned(it)
            }
        }
        return other == TolkTy.Never
    }

    override fun toString(): String = "array<$elementType>"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TolkTyArray) return false
        return elementType == other.elementType
    }

    override fun isEquivalentToInner(other: TolkTy): Boolean {
        if (this == other) return true
        if (other is TolkTyAlias) return this.isEquivalentTo(other.underlyingType)
        if (other !is TolkTyArray) return false
        return elementType.isEquivalentToInner(other.elementType)
    }

    override fun hashCode(): Int {
        var result = hashCode
        if (result == 0) {
            result = 1 + elementType.hashCode()
            result = 31 * result + hasTypeAlias.hashCode()
            result = 31 * result + hasGenerics.hashCode()
            hashCode = result
        }
        return result
    }

    companion object {
        fun create(elementType: TolkTy): TolkTy {
            return TolkTyArray(elementType)
        }
    }
}
