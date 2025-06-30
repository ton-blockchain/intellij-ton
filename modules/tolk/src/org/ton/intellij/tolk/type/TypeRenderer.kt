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
            is TolkTyAlias -> if (useAliasNames) buildString {
                val psi = ty.psi
                append(psi.name ?: return anonymous)
                if (includeTypeArguments && ty.typeArguments.isNotEmpty()) {
                    append(ty.typeArguments.joinToString(", ", "<", ">", transform = render))
                }
            } else {
                render(ty.underlyingType, level)
            }
            is TolkTyBool -> "bool"
            is TolkTyCoins -> "coins"
            is TolkTyVarInt16 -> "varint16"
            is TolkTyVarInt32 -> "varint32"
            is TolkIntNTy -> if (ty.unsigned) {
                "uint${ty.n}"
            } else {
                "int${ty.n}"
            }
            is TolkIntTy -> "int"
            is TolkTyTensor -> ty.elements.joinToString(", ", "(", ")", transform = render)
            is TolkTyTypedTuple -> ty.elements.joinToString(", ", "[", "]", transform = render)
            is TolkTyFunction -> buildString {
                append("(")
                var separator = ""
                ty.parametersType.forEach {
                    append(separator)
                    append(render(it))
                    separator = ", "
                }
                append(") -> ")
                append(render(ty.returnType))
            }
            is TolkTyUnion -> buildString {
                val orNull = ty.orNull
                if (orNull != null) {
                    append(render(orNull))
                    append("?")
                } else {
                    append(ty.variants.joinToString(" | ", transform = render))
                }
            }

            is TolkTyParam -> ty.name ?: anonymous
            is TolkTyStruct -> buildString {
                append(ty.psi.name ?: return anonymous)
                if (includeTypeArguments && ty.typeArguments.isNotEmpty()) {
                    append(ty.typeArguments.joinToString(", ", "<", ">", transform = render))
                }
            }

            else -> ty.toString()
        }
    }

    companion object
}
