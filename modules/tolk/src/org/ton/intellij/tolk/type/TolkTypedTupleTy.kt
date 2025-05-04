package org.ton.intellij.tolk.type

class TolkTypedTupleTy private constructor(
    val elements: List<TolkTy>,
    private val hasGenerics: Boolean
) : TolkTy {
    override fun toString(): String = "[${elements.joinToString()}]"

    override fun hasGenerics(): Boolean = hasGenerics

    override fun renderAppendable(appendable: Appendable): Appendable {
        appendable.append("[")
        var separator = ""
        elements.forEach {
            appendable.append(separator)
            it.renderAppendable(appendable)
            separator = ", "
        }
        appendable.append("]")
        return appendable
    }

    override fun actualType(): TolkTypedTupleTy = TolkTypedTupleTy(
        elements.map { it.actualType() },
        hasGenerics
    )

    override fun superFoldWith(folder: TypeFolder): TolkTy {
        return create(
            elements.map { it.foldWith(folder) },
        )
    }

    override fun join(other: TolkTy): TolkTy {
        if (this == other) return this
        if (other is TolkTypedTupleTy && elements.size == other.elements.size) {
            var hasGenerics = false
            val joined = elements.zip(other.elements).map { (a, b) ->
                val join = a.join(b)
                hasGenerics = hasGenerics || join.hasGenerics()
                join
            }
            return TolkTypedTupleTy(joined, hasGenerics)
        }
        return TolkUnionTy.create(this, other)
    }

    override fun meet(other: TolkTy): TolkTy {
        if (this == other) return this
        if (other == TolkTy.Unknown) return this
        if (other is TolkTypedTupleTy && elements.size == other.elements.size) {
            var hasGenerics = false
            val joined = elements.zip(other.elements).map { (a, b) ->
                val meet = a.meet(b)
                hasGenerics = hasGenerics || meet.hasGenerics()
                meet
            }
            return TolkTypedTupleTy(joined, hasGenerics)
        }
        return TolkTy.Never
    }


    override fun canRhsBeAssigned(other: TolkTy): Boolean {
        if (other is TolkTypedTupleTy) {
            if (elements.size != other.elements.size) return false
            for (i in elements.indices) {
                if (!elements[i].canRhsBeAssigned(other.elements[i])) return false
            }
            return true
        }
        if (other is TolkAliasTy) return canRhsBeAssigned(other.unwrapTypeAlias())
        return other == TolkTy.Never
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TolkTypedTupleTy

        if (hasGenerics != other.hasGenerics) return false
        if (elements != other.elements) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hasGenerics.hashCode()
        result = 31 * result + elements.hashCode()
        return result
    }

    companion object {
        fun create(vararg elements: TolkTy): TolkTy {
            return create(elements.toList())
        }

        fun create(elements: List<TolkTy>): TolkTy {
            return TolkTypedTupleTy(elements, elements.any { it.hasGenerics() })
        }
    }
}
