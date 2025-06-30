package org.ton.intellij.tolk.type


open class Substitution(
    val typeSubst: Map<TolkTyParam, TolkTy>
) {
    operator fun plus(other: Substitution): Substitution =
        Substitution(
            typeSubst + other.typeSubst
        )

    operator fun get(key: TolkTyParam) = typeSubst[key]

    open fun isEmpty() = typeSubst.isEmpty()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Substitution
        return typeSubst == other.typeSubst
    }

    override fun hashCode(): Int = typeSubst.hashCode()

    fun deduce(paramType: TolkTy, argType: TolkTy): Substitution {
        return when {
            paramType is TolkTyStruct -> {
                val argType = (argType as? TolkTyStruct)?.takeIf {
                    it.typeArguments.size == paramType.typeArguments.size
                } ?: return this
                var sub = this
                for (i in paramType.typeArguments.indices) {
                    sub = sub.deduce(paramType.typeArguments[i], argType.typeArguments[i])
                }
                return sub
            }
            paramType is TolkTyAlias -> {
                val argType = (argType as? TolkTyAlias)?.takeIf {
                    it.typeArguments.size == paramType.typeArguments.size
                } ?: return this
                var sub = this
                for (i in paramType.typeArguments.indices) {
                    sub = sub.deduce(paramType.typeArguments[i], argType.typeArguments[i])
                }
                return sub
            }

            paramType is TolkTyFunction -> {
                val argType = (argType.unwrapTypeAlias() as? TolkTyFunction)?.takeIf {
                    it.parametersType.size == paramType.parametersType.size
                } ?: return this
                var sub = this
                for (i in paramType.parametersType.indices) {
                    sub = sub.deduce(paramType.parametersType[i], argType.parametersType[i])
                }
                sub = sub.deduce(paramType.returnType, argType.returnType)
                return sub
            }

            paramType is TolkTyTensor -> {
                // `arg: (int, T)` called as `f((5, cs))` => T is slice
                val argTensor = (argType.unwrapTypeAlias() as? TolkTyTensor)?.takeIf {
                    it.elements.size == paramType.elements.size
                } ?: return this
                var sub = this
                for (i in argTensor.elements.indices) {
                    val paramItem = paramType.elements[i]
                    val argItem = argTensor.elements[i]
                    sub = sub.deduce(paramItem, argItem)
                }
                return sub
            }

            paramType is TolkTyTypedTuple -> {
                // `arg: [int, T]` called as `f([5, cs])` => T is slice
                val argTuple = (argType.unwrapTypeAlias() as? TolkTyTypedTuple)?.takeIf {
                    it.elements.size == paramType.elements.size
                } ?: return this
                var sub = this
                for (i in argTuple.elements.indices) {
                    val paramItem = paramType.elements[i]
                    val argItem = argTuple.elements[i]
                    sub = sub.deduce(paramItem, argItem)
                }
                return sub
            }

            paramType is TolkTyUnion -> {
                val argUnion = argType.unwrapTypeAlias() as? TolkTyUnion
                if (argUnion == null) {
                    // `arg: int | MyData<T>` called as `f(MyData<int>)` => T is int
                    var sub = this
                    for (paramVariant in paramType.variants) {
                        sub = sub.deduce(paramVariant, argType)
                    }
                    return sub
                }
                // `arg: T1 | T2` called as `f(intOrBuilder)` => T1 is int, T2 is builder
                // `arg: int | T1` called as `f(builderOrIntOrSlice)` => T1 is builder|slice
                val aSubP = argUnion.variants.toMutableList()
                val paramGenerics = ArrayList<TolkTy>(aSubP.size)
                var isSubCorrect = true
                for (paramVariant in paramType.variants) {
                    if (paramVariant.hasGenerics()) {
                        paramGenerics.add(paramVariant)
                    } else if (!aSubP.remove(paramVariant)) {
                        isSubCorrect = false
                    }
                }
                if (isSubCorrect && paramGenerics.size == 1 && aSubP.size > 1) {
                    return deduce(paramGenerics[0], TolkTyUnion.create(aSubP))
                } else if (isSubCorrect && paramGenerics.size == aSubP.size) {
                    var sub = this
                    for (i in paramGenerics.indices) {
                        val paramItem = paramGenerics[i]
                        val argItem = aSubP[i]
                        sub = sub.deduce(paramItem, argItem)
                    }
                    return sub
                } else {
                    return this
                }
            }

            paramType is TolkTyParam -> {
                if (!typeSubst.containsKey(paramType)) {
                    val newType =
                        if ((argType == paramType) && paramType.parameter is TolkTyParam.NamedTypeParameter) {
                            paramType.parameter.psi.defaultTypeParameter?.typeExpression?.type ?: return this
                        } else {
                            argType
                        }

                    val size = typeSubst.size
                    if (size == 0) {
                        return Substitution(mapOf(paramType to newType))
                    }
                    val map = HashMap<TolkTyParam, TolkTy>(size + 1)
                    map.putAll(typeSubst)
                    map[paramType] = newType
                    Substitution(map)
                } else {
                    this
                }
            }

            else -> this
        }
    }

    companion object {
        fun empty(): Substitution = EmptySubstitution

        fun instantiate(paramType: TolkTy, argType: TolkTy): Substitution {
            return EmptySubstitution.deduce(paramType, argType)
        }
    }
}

object EmptySubstitution : Substitution(emptyMap()) {
    override fun isEmpty(): Boolean = true
}

fun Map<TolkTyParam, TolkTy>.toSubstitution() = Substitution(this)
