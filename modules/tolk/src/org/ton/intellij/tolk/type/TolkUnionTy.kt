package org.ton.intellij.tolk.type

import org.ton.intellij.tolk.psi.TolkElement

class TolkUnionTy private constructor(
    val variants: Set<TolkTy>,
    private val hasGenerics: Boolean,
) : TolkTy {
    override fun hasGenerics(): Boolean = hasGenerics

    override fun superFoldWith(folder: TypeFolder): TolkTy {
        return create(variants.map { it.foldWith(folder) })
    }

    val orNull: TolkTy?
        get() {
            if (variants.size != 2) return null
            val first = variants.first()
            val second = variants.last()
            if (first == TolkTy.Null) return second
            if (second == TolkTy.Null) return first
            return null
        }

    override fun toString(): String {
        val orNull = orNull
        if (orNull != null) {
            return "$orNull?"
        }
        return variants.joinToString(" | ")
    }

    override fun removeNullability(): TolkTy {
        return create(variants.filter { it != TolkTy.Null })
    }

    override fun isSuperType(other: TolkTy): Boolean {
        return variants.all { it.isSuperType(other) }
    }

    override fun join(other: TolkTy): TolkTy {
        if (other is TolkUnionTy) {
            return create(variants + other.variants)
        }
        return create(variants + other)
    }

    override fun meet(other: TolkTy): TolkTy {
        if (other is TolkUnionTy) {
            return create(variants.intersect(other.variants))
        }
        return create(variants.filter { it.isSuperType(other) })
    }

    override fun substitute(substitution: Map<TolkElement, TolkTy>): TolkTy {
        val newElements = LinkedHashSet<TolkTy>()
        variants.forEach {
            newElements.add(it.substitute(substitution))
        }
        return simplify(newElements)
    }

    override fun isEquivalentToInner(other: TolkTy): Boolean {
        if (this === other) return true
        if (other !is TolkUnionTy) return false
        return containsAll(other)
    }

    fun containsAll(rhsType: TolkUnionTy): Boolean {
        for (rhsVariant in rhsType.variants) {
            if (!contains(rhsVariant)) {
                return false
            }
        }
        return true
    }

    operator fun contains(type: TolkTy): Boolean {
        return variants.any { it.actualType().isEquivalentTo(type.actualType()) }
    }

    override fun canRhsBeAssigned(other: TolkTy): Boolean {
        if (other == this) return true
        if (calculateExactVariantToFitRhs(other) != null) return true
        if (other is TolkUnionTy) return containsAll(other)
        if (other is TolkTypeAliasTy) return canRhsBeAssigned(other.unwrapTypeAlias())
        return other == TolkTy.Never
    }

    fun calculateExactVariantToFitRhs(
        rhsType: TolkTy
    ): TolkTy? {
        val rhsUnion = rhsType.unwrapTypeAlias() as? TolkUnionTy
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
        var firstCovering: TolkTy? = null
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
        fun create(vararg elements: TolkTy): TolkTy {
            return create(elements.toList())
        }

        fun create(elements: Collection<TolkTy>): TolkTy {
            val elements = joinUnions(elements)
            if (elements.size == 1) return elements.first()
            return TolkUnionTy(elements, elements.any { it.hasGenerics() })
        }

        private fun joinUnions(set: Collection<TolkTy>): Set<TolkTy> {
            val flatVariants = LinkedHashMap<TolkTy, TolkTy>(set.size)
            set.forEach { variant ->
                if (variant is TolkUnionTy) {
                    for (nestedVariant in variant.variants) {
                        val actualType = nestedVariant.actualType()
                        if (actualType == TolkTy.Null) {
                            flatVariants[actualType] = actualType
                        } else {
                            flatVariants.getOrPut(actualType) {
                                nestedVariant
                            }
                        }
                    }
                } else {
                    val actualType = variant.actualType()
                    if (actualType == TolkTy.Null) {
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


        private fun simplify(elements: Set<TolkTy>): TolkTy {
            when (elements.size) {
                1 -> {
                    return elements.single()
                }

                else -> {
                    val unique: MutableList<TolkTy> = ArrayList(elements)
                    var changed = true
                    while (changed) {
                        changed = false
                        outer@ for (i in 0 until unique.size) {
                            for (j in 0 until unique.size) {
                                if (i == j) continue
                                val iType = unique[i]
                                val jType = unique[j]
                                val joined = iType.join(jType)
                                if (joined !is TolkUnionTy) {
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
                    val uniqueSet = LinkedHashSet<TolkTy>(unique.size)
                    for (type in unique) {
                        if (type.hasGenerics()) {
                            hasGenerics = true
                        }
                        uniqueSet.add(type)
                    }
                    return TolkUnionTy(uniqueSet, hasGenerics)
                }
            }
        }
    }
}
