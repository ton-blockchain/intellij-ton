package org.ton.intellij.tolk.type

class TolkUnionType private constructor(
    val elements: Set<TolkType>,
    private val hasGenerics: Boolean,
) : TolkType {
    override fun hasGenerics(): Boolean = hasGenerics

    override fun toString(): String {
        if (elements.size == 2) {
            val first = elements.first()
            val second = elements.last()
            if (first == TolkType.Null) return "$second?"
            if (second == TolkType.Null) return "$first?"
        }
        return elements.joinToString(" | ")
    }

    override fun printDisplayName(appendable: Appendable) {
        if (elements.size == 2) {
            val first = elements.first()
            val second = elements.last()
            if (first == TolkType.Null) {
                second.printDisplayName(appendable)
                appendable.append("?")
                return
            }
            if (second == TolkType.Null) {
                first.printDisplayName(appendable)
                appendable.append("?")
                return
            }
        }
        var separator = ""
        elements.forEach {
            appendable.append(separator)
            it.printDisplayName(appendable)
            separator = " | "
        }
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
            when (elements.size) {
                1 -> {
                    return elements.single()
                }
                2 -> {
                    return TolkUnionType(elements, elements.any { it.hasGenerics() })
                }
                else -> {
                    val unique: MutableList<TolkType> = ArrayList(elements)
                    var changed = true
                    while (changed) {
                        changed = false
                        outer@ for (i in 0 until unique.size) {
                            for (j in 0 until unique.size) {
                                if (i == j) continue
                                val iType = unique[i]
                                val jType = unique[j]
                                val joined = iType.join(jType)
                                if (joined !is TolkUnionType) {
                                    unique[i] = joined
                                    unique.removeAt(j)
                                    changed = true
                                    break@outer
                                }
                            }
                        }
                    }

                    if (unique.size == 1) return unique.single()
                    var hasGenerics = false
                    val uniqueSet = HashSet<TolkType>(unique.size)
                    for (type in unique) {
                        if (type.hasGenerics()) {
                            hasGenerics = true
                        }
                        uniqueSet.add(type)
                    }
                    return TolkUnionType(uniqueSet, hasGenerics)
                }
            }
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
