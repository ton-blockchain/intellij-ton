package org.ton.intellij.tolk.type

import org.ton.intellij.tolk.psi.TolkTypeParameter

class TolkUnionType private constructor(
    val variants: Set<TolkType>,
    private val hasGenerics: Boolean,
) : TolkType {
    override fun hasGenerics(): Boolean = hasGenerics

    val orNull: TolkType?
        get() {
            if (variants.size != 2) return null
            val first = variants.first()
            val second = variants.last()
            if (first == TolkType.Null) return second
            if (second == TolkType.Null) return first
            return null
        }

    override fun toString(): String {
        val orNull = orNull
        if (orNull != null) {
            return "$orNull?"
        }
        return variants.joinToString(" | ")
    }

    override fun renderAppendable(appendable: Appendable) = appendable.apply {
        val orNull = orNull
        if (orNull != null) {
            orNull.renderAppendable(this)
            append("?")
            return@apply
        }
        var separator = ""
        variants.forEach {
            appendable.append(separator)
            it.renderAppendable(appendable)
            separator = " | "
        }
    }

    override fun removeNullability(): TolkType {
        return create(variants.filter { it != TolkType.Null })
    }

    override fun isSuperType(other: TolkType): Boolean {
        return variants.all { it.isSuperType(other) }
    }

    override fun join(other: TolkType): TolkType {
        if (other is TolkUnionType) {
            return create(variants + other.variants)
        }
        return create(variants + other)
    }

    override fun meet(other: TolkType): TolkType {
        if (other is TolkUnionType) {
            return create(variants.intersect(other.variants))
        }
        return create(variants.filter { it.isSuperType(other) })
    }

    override fun visit(visitor: TolkTypeVisitor) {
        variants.forEach { it.visit(visitor) }
    }

    override fun substitute(substitution: Map<TolkTypeParameter, TolkType>): TolkType {
        val newElements = LinkedHashSet<TolkType>()
        variants.forEach {
            newElements.add(it.substitute(substitution))
        }
        return simplify(newElements)
    }

    fun containsAll(rhsType: TolkUnionType): Boolean {
        for (rhsVariant in rhsType.variants) {
            if (!contains(rhsVariant)) {
                return false
            }
        }
        return true
    }

    operator fun contains(type: TolkType): Boolean {
        return variants.any { it.actualType() == type.actualType() }
    }

    override fun canRhsBeAssigned(other: TolkType): Boolean {
        if (other == this) return true
        if (calculateExactVariantToFitRhs(other) != null) return true
        if (other is TolkUnionType) return containsAll(other)
        if (other is TolkAliasType) return canRhsBeAssigned(other.unwrapTypeAlias())
        return other == TolkType.Never
    }

    fun calculateExactVariantToFitRhs(
        rhsType: TolkType
    ): TolkType? {
        val rhsUnion = rhsType.unwrapTypeAlias() as? TolkUnionType
        //   // primitive 1-slot nullable don't store type_id, they can be assigned less strict, like `int?` to `int16?`
        if (rhsUnion != null) {
            val orNull = orNull
            val rhsOrNull = rhsUnion.orNull
            if (orNull != null && rhsOrNull != null && orNull.canRhsBeAssigned(rhsOrNull)) {
                return this
            }
            return null
        }
        // `int` to `int | int8` is okay: exact type matching
        for (variant in variants) {
            if (variant.actualType() == rhsType.actualType()) {
                return variant
            }
        }
        // find the only T_i; it would also be used for transition at IR generation, like `(int,null)` to `(int, User?) | int`
        var firstCovering: TolkType? = null
        for (variant in variants) {
            if (variant.canRhsBeAssigned(rhsType)) {
                if (firstCovering != null) {
                    return null
                }
                firstCovering = variant
            }
        }
        return firstCovering
    }

    companion object {
        fun create(vararg elements: TolkType): TolkType {
            return create(elements.toList())
        }

        fun create(elements: Collection<TolkType>): TolkType {
            val elements = joinUnions(elements)
            if (elements.size == 1) return elements.first()
            return TolkUnionType(elements, elements.any { it.hasGenerics() })
        }

        private fun joinUnions(set: Collection<TolkType>): Set<TolkType> {
            val flatVariants = LinkedHashMap<TolkType, TolkType>(set.size)
            set.forEach { variant ->
                if (variant is TolkUnionType) {
                    for (nestedVariant in variant.variants) {
                        val actualType = nestedVariant.actualType()
                        if (actualType == TolkType.Null) {
                            flatVariants[actualType] = actualType
                        } else {
                            flatVariants.getOrPut(actualType) {
                                nestedVariant
                            }
                        }
                    }
                } else {
                    val actualType = variant.actualType()
                    if (actualType == TolkType.Null) {
                        flatVariants[actualType] = actualType
                    } else {
                        flatVariants.getOrPut(actualType) {
                            variant
                        }
                    }
                }
            }
            return flatVariants.values.toSet()
        }


        private fun simplify(elements: Set<TolkType>): TolkType {
            when (elements.size) {
                1 -> {
                    return elements.single()
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
                    val uniqueSet = LinkedHashSet<TolkType>(unique.size)
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
    }
}
