package org.ton.intellij.tolk.type

class TolkUnionType private constructor(
    val elements: Set<TolkType>
) : TolkType {
    override fun toString(): String {
        if (elements.size == 2) {
            val first = elements.first()
            val second = elements.last()
            if (first == TolkType.Null) return "$second?"
            if (second == TolkType.Null) return "$first?"
        }
        return elements.joinToString(" | ")
    }

    override fun removeNullability(): TolkType {
        return create(elements.asSequence().filter { it != TolkType.Null }.asIterable())
    }

    override fun isSuperType(other: TolkType): Boolean {
        return elements.all { it.isSuperType(other) }
    }

    override fun join(other: TolkType): TolkType {
        if (other is TolkUnionType) {
            return create(elements + other.elements)
        }
        return create(elements + other)
    }

    override fun meet(other: TolkType): TolkType {
        if (other is TolkUnionType) {
            return create(elements.intersect(other.elements))
        }
        return create(elements.filter { it.isSuperType(other) })
    }

    override fun visit(visitor: TolkTypeVisitor) {
        elements.forEach { it.visit(visitor) }
    }

    companion object {
        fun create(vararg elements: TolkType): TolkType {
            return create(elements.asIterable())
        }

        fun create(elements: Iterable<TolkType>): TolkType {
            val elements = joinUnions(elements)
            return elements.singleOrNull() ?: TolkUnionType(elements)
        }

        private fun joinUnions(set: Iterable<TolkType>): Set<TolkType> {
            val newElements = mutableSetOf<TolkType>()
            set.forEach { element ->
                if (element is TolkUnionType) {
                    newElements.addAll(element.elements)
                } else {
                    newElements.add(element)
                }
            }
            return newElements
        }
    }
}
