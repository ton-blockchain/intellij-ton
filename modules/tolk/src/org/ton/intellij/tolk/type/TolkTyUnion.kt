package org.ton.intellij.tolk.type

class TolkTyUnion private constructor(
    val variants: Collection<TolkTy>,
    override val hasTypeAlias: Boolean = variants.any { it.hasTypeAlias }
) : TolkTy {
    private var hashCode: Int = 0

    private val hasGenerics: Boolean = variants.any { it.hasGenerics() }

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

    override fun hashCode(): Int {
        var result = hashCode
        if (result == 0) {
            result = variants.hashCode()
            hashCode = result
        }
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TolkTyUnion) return false
        if (hasGenerics != other.hasGenerics) return false
        if (hasTypeAlias != other.hasTypeAlias) return false
        if (variants.size != other.variants.size) return false
        if (hashCode != 0 && other.hashCode != 0 && hashCode != other.hashCode) return false
        return variants == other.variants
    }

    override fun isSuperType(other: TolkTy): Boolean {
        return variants.all { it.isSuperType(other) }
    }

    override fun isEquivalentToInner(other: TolkTy): Boolean {
        if (this === other) return true
        if (other !is TolkTyUnion) return false
        return containsAll(other)
    }

    fun containsAll(rhsType: TolkTyUnion): Boolean {
        for (rhsVariant in rhsType.variants) {
            if (!contains(rhsVariant)) {
                return false
            }
        }
        return true
    }

    operator fun contains(type: TolkTy): Boolean {
        val actualType = type.unwrapTypeAlias().actualType()
        return variants.any { it.isEquivalentTo(actualType) }
    }

    override fun canRhsBeAssigned(other: TolkTy): Boolean {
        if (other == this) return true
        if (calculateExactVariantToFitRhs(other) != null) return true
        if (other is TolkTyUnion) return containsAll(other)
        if (other is TolkTyAlias) return canRhsBeAssigned(other.unwrapTypeAlias())
        return other == TolkTy.Never
    }

    fun calculateExactVariantToFitRhs(
        rhsType: TolkTy
    ): TolkTy? {
        val rhsUnion = rhsType.unwrapTypeAlias() as? TolkTyUnion
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
            return flattenVariants(elements)
        }

        private fun flattenVariants(variants: Collection<TolkTy>): TolkTy {
            check(variants.isNotEmpty()) { "Cannot create union from an empty set of types" }
            val flatVariants = ArrayList<TolkTy>(variants.size)
            for (variant in variants) {
                val nestedUnion = (variant.unwrapTypeAlias() as? TolkTyUnion)
                if (nestedUnion != null) {
                    nestedUnion.variants.forEach {
                        flatVariants.addUniqueType(it)
                    }
                } else {
                    flatVariants.addUniqueType(variant)
                }
            }
            if (flatVariants.size == 1) {
                return flatVariants.first()
            }
            return TolkTyUnion(flatVariants)
        }

        private fun MutableCollection<TolkTy>.addUniqueType(variant: TolkTy) {
            val actualType = variant.unwrapTypeAlias().actualType()
            for (existing in this) {
                if (existing.unwrapTypeAlias().actualType() == actualType) {
                    return
                }
            }
            this.add(variant)
        }

        private fun joinUnions(set: Collection<TolkTy>): Set<TolkTy> {
            val flatVariants = LinkedHashMap<TolkTy, TolkTy>(set.size)
            set.forEach { variant ->
                if (variant is TolkTyUnion) {
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
    }
}
