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

    fun deduce(paramType: TolkTy, argType: TolkTy, applyDefault: Boolean = true): Substitution {
        return when {
            paramType is TolkTyStruct -> {
                var sub = this
                // `arg: Wrapper<T>` called as `f(wrappedInt)` => T is int
                val unwrappedArgType = argType.unwrapTypeAlias() as? TolkTyStruct
                if (unwrappedArgType != null && unwrappedArgType.typeArguments.size == paramType.typeArguments.size) {
                    for (i in paramType.typeArguments.indices) {
                        sub = sub.deduce(paramType.typeArguments[i], unwrappedArgType.typeArguments[i], applyDefault)
                    }
                }

                // `arg: Wrapper<T>` called as `f(Wrapper<Wrapper<T>>)` => T is Wrapper<T>
                val argType = argType as? TolkTyStruct
                if (argType != null && argType.typeArguments.size == paramType.typeArguments.size) {
                    for (i in paramType.typeArguments.indices) {
                        sub = sub.deduce(paramType.typeArguments[i], argType.typeArguments[i], applyDefault)
                    }
                }
                return sub
            }
            paramType is TolkTyAlias -> {
                // Since map<K, V> is just a type alias in stdlib, we handle
                // ```
                // if (const auto* p_map = param_type->try_as<TypeDataMapKV>()) {
                //    // `arg: map<K, V>` called as `f(someMapInt32Slice)` => K = int32, V = slice
                //    if (const auto* a_map = arg_type->unwrap_alias()->try_as<TypeDataMapKV>()) {
                //      consider_next_condition(p_map->TKey, a_map->TKey);
                //      consider_next_condition(p_map->TValue, a_map->TValue);
                //    }
                //  }
                // ```
                // here as well:
                var sub = this
                // `arg: Wrapper<T>` called as `f(wrappedInt)` => T is int
                val unwrappedArgType = argType.unwrapTypeAlias() as? TolkTyAlias
                if (unwrappedArgType != null && unwrappedArgType.typeArguments.size == paramType.typeArguments.size) {
                    for (i in paramType.typeArguments.indices) {
                        sub = sub.deduce(paramType.typeArguments[i], unwrappedArgType.typeArguments[i], applyDefault)
                    }
                }

                // `arg: Wrapper<T>` called as `f(Wrapper<Wrapper<T>>)` => T is Wrapper<T>
                val argType = argType as? TolkTyAlias
                if (argType != null && argType.typeArguments.size == paramType.typeArguments.size) {
                    for (i in paramType.typeArguments.indices) {
                        sub = sub.deduce(paramType.typeArguments[i], argType.typeArguments[i], applyDefault)
                    }
                }
                return sub
            }

            paramType is TolkTyFunction -> {
                val argType = (argType.unwrapTypeAlias() as? TolkTyFunction)?.takeIf {
                    it.parametersType.size == paramType.parametersType.size
                } ?: return this
                var sub = this
                for (i in paramType.parametersType.indices) {
                    sub = sub.deduce(paramType.parametersType[i], argType.parametersType[i], applyDefault)
                }
                sub = sub.deduce(paramType.returnType, argType.returnType, applyDefault)
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
                    sub = sub.deduce(paramItem, argItem, applyDefault)
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
                    sub = sub.deduce(paramItem, argItem, applyDefault)
                }
                return sub
            }

            paramType is TolkTyUnion -> {
                val argUnion = argType.unwrapTypeAlias() as? TolkTyUnion
                if (argUnion == null) {
                    // `arg: int | MyData<T>` called as `f(MyData<int>)` => T is int
                    var sub = this
                    for (paramVariant in paramType.variants) {
                        sub = sub.deduce(paramVariant, argType, applyDefault)
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
                    return deduce(paramGenerics[0], TolkTyUnion.create(aSubP), applyDefault)
                } else if (isSubCorrect && paramGenerics.size == aSubP.size) {
                    var sub = this
                    for (i in paramGenerics.indices) {
                        val paramItem = paramGenerics[i]
                        val argItem = aSubP[i]
                        sub = sub.deduce(paramItem, argItem, applyDefault)
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
                            if (!applyDefault) {
                                return this
                            }
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

        fun instantiate(paramType: TolkTy, argType: TolkTy, applyDefault: Boolean = true): Substitution {
            return EmptySubstitution.deduce(paramType, argType, applyDefault)
        }
    }
}

object EmptySubstitution : Substitution(emptyMap()) {
    override fun isEmpty(): Boolean = true
}

fun Map<TolkTyParam, TolkTy>.toSubstitution() = Substitution(this)
