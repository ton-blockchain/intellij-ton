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
            paramType is TolkStructTy && argType is TolkStructTy -> {
                paramType.typeArguments.asSequence().zip(argType.typeArguments.asSequence()).fold(this) { sub, (a, b) ->
                    sub.deduce(a.unwrapTypeAlias(), b.unwrapTypeAlias())
                }
            }

            paramType is TolkTyFunction && argType is TolkTyFunction -> {
                paramType.parametersType.asSequence()
                    .zip(argType.parametersType.asSequence())
                    .fold(this) { sub, (a, b) ->
                        sub.deduce(a.unwrapTypeAlias(), b.unwrapTypeAlias())
                    } + deduce(paramType.returnType.unwrapTypeAlias(), argType.returnType.unwrapTypeAlias())
            }

            paramType is TolkTyTensor && argType is TolkTyTensor -> {
                paramType.elements.asSequence().zip(argType.elements.asSequence())
                    .fold(this) { sub, (a, b) ->
                        sub.deduce(a.unwrapTypeAlias(), b.unwrapTypeAlias())
                    }
            }

            paramType is TolkTyTypedTuple && argType is TolkTyTypedTuple -> {
                paramType.elements.asSequence().zip(argType.elements.asSequence())
                    .fold(this) { sub, (a, b) ->
                        sub.deduce(a.unwrapTypeAlias(), b.unwrapTypeAlias())
                    }
            }

            paramType is TolkTyUnion && argType is TolkTyUnion -> {
                paramType.variants.asSequence().zip(argType.variants.asSequence())
                    .fold(this) { sub, (a, b) ->
                        sub.deduce(a.actualType(), b.actualType())
                    }
            }

            paramType is TolkTyParam -> {
                if (!typeSubst.containsKey(paramType)) {
                    val newType =
                        if ((argType == paramType || argType == TolkTy.Unknown) && paramType.parameter is TolkTyParam.NamedTypeParameter) {
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
