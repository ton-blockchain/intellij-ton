package org.ton.intellij.tolk.type

data class TolkTensorType private constructor(
    val elements: List<TolkType>,
    val hasGenerics: Boolean
) : TolkType {
    override fun toString(): String = "(${elements.joinToString()})"

    override fun printDisplayName(appendable: Appendable) = appendable.apply{
        append("(")
        var separator = ""
        elements.forEach {
            append(separator)
            it.printDisplayName(appendable)
            separator = ", "
        }
        append(")")
    }

    override fun join(other: TolkType): TolkType {
        if (this == other) return this
        if (other is TolkTensorType && elements.size == other.elements.size) {
            var hasGenerics = false
            val newElements = elements.zip(other.elements).map { (a, b) ->
                val join = a.join(b)
                hasGenerics = hasGenerics || join.hasGenerics()
                join
            }
            return TolkTensorType(newElements, hasGenerics)
        }
        return TolkUnionType.create(this, other)
    }

    override fun meet(other: TolkType): TolkType {
        if (this == other) return this
        if (other == TolkType.Unknown) return this
        if (other is TolkTensorType && elements.size == other.elements.size) {
            var hasGenerics = false
            val newElements = elements.zip(other.elements).map { (a, b) ->
                val meet = a.meet(b)
                hasGenerics = hasGenerics || meet.hasGenerics()
                meet
            }
            return TolkTensorType(newElements, hasGenerics)
        }
        return TolkType.Never
    }

    override fun visit(visitor: TolkTypeVisitor) {
        elements.forEach { it.visit(visitor) }
    }

    companion object {
        fun create(vararg elements: TolkType): TolkType {
            return create(elements.toList())
        }

        fun create(elements: List<TolkType>): TolkType {
            if (elements.isEmpty()) return TolkUnitType
            if (elements.size == 1) {
                return elements.single()
            }
            val hasGenerics = elements.any { it.hasGenerics() }
            return TolkTensorType(elements, hasGenerics)
        }
    }
}
