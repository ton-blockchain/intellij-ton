package org.ton.intellij.tolk.type

class TolkUnionTy private constructor(
    val variants: Set<TolkTy>,
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
        if (other !is TolkUnionTy) return false
        if (variants.size != other.variants.size) return false
        if (hashCode != 0 && other.hashCode != 0 && hashCode != other.hashCode) return false
        return variants == other.variants
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
            if (elements.size == 1) return elements.first()
            val elements = joinUnions(elements)
            if (elements.size == 1) return elements.first()
            return TolkUnionTy(elements)
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
    }
}
