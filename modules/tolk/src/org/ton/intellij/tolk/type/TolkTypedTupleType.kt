package org.ton.intellij.tolk.type

data class TolkTypedTupleType(
    val elements: List<TolkType>
) : TolkType {
    override fun toString(): String = "[${elements.joinToString()}]"

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
            return TolkTypedTupleType(elements.zip(other.elements).map { (a, b) -> a.join(b) })
        }
        return TolkUnionType.create(this, other)
    }

    override fun meet(other: TolkType): TolkType {
        if (this == other) return this
        if (other == TolkType.Unknown) return this
        if (other is TolkTypedTupleType && elements.size == other.elements.size) {
            return TolkTypedTupleType(elements.zip(other.elements).map { (a, b) -> a.meet(b) })
        }
        return TolkType.Never
    }

    override fun visit(visitor: TolkTypeVisitor) {
        elements.forEach { it.visit(visitor) }
    }
}
