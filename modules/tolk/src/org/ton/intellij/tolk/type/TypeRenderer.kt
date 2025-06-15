package org.ton.intellij.tolk.type

fun TolkTy.render(
    level: Int = Int.MAX_VALUE,
    unknown: String = "<unknown>",
    anonymous: String = "<anonymous>",
    includeTypeArguments: Boolean = true,
    useAliasNames: Boolean = true,
) = TypeRenderer(
    unknown = unknown,
    anonymous = anonymous,
    includeTypeArguments = includeTypeArguments,
    useAliasNames = useAliasNames
).render(this, level)

private class TypeRenderer(
    val unknown: String,
    val anonymous: String,
    val includeTypeArguments: Boolean,
    val useAliasNames: Boolean,
) {
    fun render(ty: TolkTy, level: Int): String {
        if (ty == TolkTy.Unknown) return unknown

        if (level <= 0) return "…"

        val render = { subTy: TolkTy ->
            render(subTy, level - 1)
        }

        return when (ty) {
            is TolkTypeAliasTy -> if (useAliasNames) buildString {
                val psi = ty.psi
                append(psi.name ?: return anonymous)
                if (includeTypeArguments) {
                    val typeParameters = psi.typeParameterList?.typeParameterList
                    if (typeParameters != null && typeParameters.isNotEmpty()) {
                        append(
                            typeParameters.joinToString(", ", "<", ">") { typeParameter ->
                                typeParameter.type?.let { render(it) } ?: return@joinToString anonymous
                            }
                        )
                    }
                }
            } else {
                render(ty.underlyingType, level)
            }
            is TolkBoolTy -> "bool"
            is TolkCellTy -> "cell"
            is TolkCoinsTy -> "coins"
            is TolkVarInt16Ty -> "varint16"
            is TolkVarInt32Ty -> "varint32"
            is TolkIntNTy -> if (ty.unsigned) {
                "uint${ty.n}"
            } else {
                "int${ty.n}"
            }
            is TolkBytesNTy -> "bytes${ty.n}"
            is TolkIntTy -> "int"
            is TolkBuilderTy -> "builder"
            is TolkVoidTy -> "void"
            is TolkTensorTy -> ty.elements.joinToString(", ", "(", ")", transform = render)
            is TolkTypedTupleTy -> ty.elements.joinToString(", ", "[", "]", transform = render)
            is TolkFunctionTy -> {
                if (ty.inputType == TolkVoidTy) {
                    "() -> ${render(ty.returnType)}"
                } else {
                    "${render(ty.inputType)} -> ${render(ty.returnType)}"
                }
            }
            is TolkUnionTy -> buildString {
                val orNull = ty.orNull
                if (orNull != null) {
                    render(orNull)
                    append("?")
                } else {
                    append(ty.variants.joinToString(" | ", transform = render))
                }
            }

            is TolkTypeParameterTy -> ty.name ?: anonymous
            is TolkStructTy -> buildString {
                append(ty.psi.name ?: return anonymous)
                if (includeTypeArguments && ty.typeArguments.isNotEmpty()) {
                    append(ty.typeArguments.joinToString(", ", "<", ">", transform = render))
                }
            }

            else -> ty.toString()
        }
    }
}
