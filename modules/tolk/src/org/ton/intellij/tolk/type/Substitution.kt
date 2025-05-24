package org.ton.intellij.tolk.type


open class Substitution(
    val typeSubst: Map<TolkTypeParameterTy, TolkTy> = emptyMap()
) {
    operator fun plus(other: Substitution): Substitution =
        Substitution(
            typeSubst + other.typeSubst
        )

    operator fun get(key: TolkTypeParameterTy) = typeSubst[key]

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
                paramType.typeArguments.zip(argType.typeArguments).fold(this) { sub, (a, b) ->
                    sub.deduce(a.unwrapTypeAlias(), b.unwrapTypeAlias())
                }
            }

            paramType is TolkFunctionTy && argType is TolkFunctionTy -> {
                deduce(
                    paramType.inputType.unwrapTypeAlias(),
                    argType.inputType.unwrapTypeAlias()
                ) + deduce(paramType.returnType.unwrapTypeAlias(), argType.returnType.unwrapTypeAlias())
            }

            paramType is TolkTensorTy && argType is TolkTensorTy -> {
                paramType.elements.zip(argType.elements)
                    .fold(this) { sub, (a, b) -> sub.deduce(a.unwrapTypeAlias(), b.unwrapTypeAlias()) }
            }

            paramType is TolkTypedTupleTy && argType is TolkTypedTupleTy -> {
                paramType.elements.zip(argType.elements)
                    .fold(this) { sub, (a, b) -> sub.deduce(a.unwrapTypeAlias(), b.unwrapTypeAlias()) }
            }

            paramType is TolkUnionTy && argType is TolkUnionTy -> {
                paramType.variants.zip(argType.variants).fold(this) { sub, (a, b) ->
                    deduce(a.actualType(), b.actualType())
                }
            }

            paramType is TolkTypeParameterTy -> {
                if (!typeSubst.containsKey(paramType)) {
                    val newType =
                        if ((argType == paramType || argType == TolkTy.Unknown) && paramType.parameter is TolkTypeParameterTy.NamedTypeParameter) {
                            paramType.parameter.psi.defaultTypeParameter?.typeExpression?.type ?: return this
                        } else {
                            argType
                        }

                    val size = typeSubst.size
                    if (size == 0) {
                        return Substitution(mapOf(paramType to newType))
                    }
                    val map = HashMap<TolkTypeParameterTy, TolkTy>(size + 1)
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
        fun instantiate(paramType: TolkTy, argType: TolkTy): Substitution {
            return EmptySubstitution.deduce(paramType, argType)
        }
    }
}

object EmptySubstitution : Substitution()

fun Map<TolkTypeParameterTy, TolkTy>.toSubstitution() = Substitution(this)
