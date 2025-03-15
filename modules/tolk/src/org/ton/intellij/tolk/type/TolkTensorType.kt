package org.ton.intellij.tolk.type

data class TolkTensorType private constructor(
    val elements: List<TolkType>
) : TolkType {
    override fun toString(): String = "(${elements.joinToString()})"

    override fun printDisplayName(appendable: Appendable) {
        appendable.append("(")
        var separator = ""
        elements.forEach {
            appendable.append(separator)
            it.printDisplayName(appendable)
            separator = ", "
        }
        appendable.append(")")
    }

    override fun join(other: TolkType): TolkType {
        if (this == other) return this
        if (other is TolkTensorType && elements.size == other.elements.size) {
            return TolkTensorType(elements.zip(other.elements).map { (a, b) -> a.join(b) })
        }
        return TolkUnionType.create(this, other)
    }

    override fun meet(other: TolkType): TolkType {
        if (this == other) return this
        if (other == TolkType.Unknown) return this
        if (other is TolkTensorType && elements.size == other.elements.size) {
            return TolkTensorType(elements.zip(other.elements).map { (a, b) -> a.meet(b) })
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

        fun create(elements: Collection<TolkType>): TolkType {
            if (elements.isEmpty()) return TolkUnitType
            if (elements.size == 1) {
                return elements.single()
            }
            return TolkTensorType(elements.toList())
        }
    }
}
