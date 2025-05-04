package org.ton.intellij.tolk.type


open class Substitution(
    val typeSubst: Map<TyTypeParameter, TolkTy> = emptyMap()
) {
    operator fun plus(other: Substitution): Substitution =
        Substitution(
            typeSubst + other.typeSubst
        )

    operator fun get(key: TyTypeParameter) = typeSubst[key]

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Substitution
        return typeSubst == other.typeSubst
    }

    override fun hashCode(): Int = typeSubst.hashCode()

    companion object {
        fun instantiate(paramType: TolkTy, argType: TolkTy): Substitution {
            val substitution = mutableMapOf<TyTypeParameter, TolkTy>()

            fun deduce(paramType: TolkTy, argType: TolkTy) {
                when {
                    paramType is TyStruct && argType is TyStruct -> {
                        paramType.typeArguments.zip(argType.typeArguments).forEach { (a, b) ->
                            deduce(a.unwrapTypeAlias(), b.unwrapTypeAlias())
                        }
                    }

                    paramType is TolkFunctionTy && argType is TolkFunctionTy -> {
                        deduce(paramType.inputType.unwrapTypeAlias(), argType.inputType.unwrapTypeAlias())
                        deduce(paramType.returnType.unwrapTypeAlias(), argType.returnType.unwrapTypeAlias())
                    }

                    paramType is TolkTensorTy && argType is TolkTensorTy -> {
                        paramType.elements.zip(argType.elements)
                            .forEach { (a, b) -> deduce(a.unwrapTypeAlias(), b.unwrapTypeAlias()) }
                    }

                    paramType is TolkTypedTupleTy && argType is TolkTypedTupleTy -> {
                        paramType.elements.zip(argType.elements)
                            .forEach { (a, b) -> deduce(a.unwrapTypeAlias(), b.unwrapTypeAlias()) }
                    }

                    paramType is TyUnion && argType is TyUnion -> {
                        paramType.variants.zip(argType.variants).forEach { (a, b) ->
                            deduce(a.unwrapTypeAlias(), b.unwrapTypeAlias())
                        }
                    }

                    paramType is TyTypeParameter -> {
                        if (!substitution.containsKey(paramType)) {
                            val newType =
                                if (argType == TolkTy.Unknown && paramType.parameter is TyTypeParameter.NamedTypeParameter) {
                                    paramType.parameter.psi.defaultTypeParameter?.typeExpression?.type
                                } else {
                                    argType
                                }
                            if (newType != null) {
                                substitution[paramType] = newType
                            }
                        }
                    }

                    else -> {}
                }
            }

            deduce(paramType.unwrapTypeAlias(), argType.unwrapTypeAlias())
            return Substitution(substitution)
        }
    }
}

object EmptySubstitution : Substitution()

fun Map<TyTypeParameter, TolkTy>.toSubstitution() = Substitution(this)
