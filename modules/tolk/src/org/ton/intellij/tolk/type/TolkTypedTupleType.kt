package org.ton.intellij.tolk.type

class TolkTypedTupleType private constructor(
    val elements: List<TolkType>,
    private val hasGenerics: Boolean
) : TolkType {
    override fun toString(): String = "[${elements.joinToString()}]"

    override fun hasGenerics(): Boolean = hasGenerics

    override fun printDisplayName(appendable: Appendable) {
        appendable.append("[")
        var separator = ""
        elements.forEach {
            appendable.append(separator)
            it.printDisplayName(appendable)
            separator = ", "
        }
        appendable.append("]")
    }

    override fun join(other: TolkType): TolkType {
        if (this == other) return this
        if (other is TolkTypedTupleType && elements.size == other.elements.size) {
            var hasGenerics = false
            val joined = elements.zip(other.elements).map { (a, b) ->
                val join = a.join(b)
                hasGenerics = hasGenerics || join.hasGenerics()
                join
            }
            return TolkTypedTupleType(joined, hasGenerics)
        }
        return TolkUnionType.create(this, other)
    }

    override fun meet(other: TolkType): TolkType {
        if (this == other) return this
        if (other == TolkType.Unknown) return this
        if (other is TolkTypedTupleType && elements.size == other.elements.size) {
            var hasGenerics = false
            val joined = elements.zip(other.elements).map { (a, b) ->
                val meet = a.meet(b)
                hasGenerics = hasGenerics || meet.hasGenerics()
                meet
            }
            return TolkTypedTupleType(joined, hasGenerics)
        }
        return TolkType.Never
    }

    override fun visit(visitor: TolkTypeVisitor) {
        elements.forEach { it.visit(visitor) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TolkTypedTupleType

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
        fun create(vararg elements: TolkType): TolkType {
            return create(elements.toList())
        }

        fun create(elements: List<TolkType>): TolkType {
            return TolkTypedTupleType(elements, elements.any { it.hasGenerics() })
        }
    }
}
