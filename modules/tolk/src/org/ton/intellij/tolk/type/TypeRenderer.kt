package org.ton.intellij.tolk.type

fun TolkTy.render(
    level: Int = Int.MAX_VALUE,
    unknown: String = "<unknown>",
    anonymous: String = "<anonymous>",
    includeTypeArguments: Boolean = true,
) = TypeRenderer(
    unknown = unknown,
    anonymous = anonymous,
    includeTypeArguments = includeTypeArguments,
).render(this, level)

private class TypeRenderer(
    val unknown: String,
    val anonymous: String,
    val includeTypeArguments: Boolean = true,
) {
    fun render(ty: TolkTy, level: Int): String {
        if (ty == TolkTy.Unknown) return unknown

        if (level <= 0) return "…"

        val render = { subTy: TolkTy ->
            render(subTy, level - 1)
        }

        return when (ty) {
            is TolkBoolTy -> "bool"
            is TolkCellTy -> "cell"
            is TolkCoinsTy -> "coins"
            is TolkIntNTy -> if (ty.unsigned) {
                "uint${ty.n}"
            } else {
                "int${ty.n}"
            }
            is TolkBytesNTy -> "bytes${ty.n}"
            is TolkIntTy -> "int"
            is TolkBuilderTy -> "builder"
            is TolkTensorTy -> ty.elements.joinToString(", ", "(", ")", transform = render)
            is TolkTypedTupleTy -> ty.elements.joinToString(", ", "[", "]", transform = render)
            is TolkFunctionTy -> "${render(ty.inputType)} -> ${render(ty.returnType)}"
            is TyUnion -> buildString {
                val orNull = ty.orNull
                if (orNull != null) {
                    render(orNull)
                    append("?")
                } else {
                    append(ty.variants.joinToString(" | ", transform = render))
                }
            }
            is TyTypeParameter -> ty.name ?: anonymous
            is TyStruct -> buildString {
                append(ty.psi.name ?: return anonymous)
                if (includeTypeArguments && ty.typeArguments.isNotEmpty()) {
                    append(ty.typeArguments.joinToString(", ", "<", ">", transform = render))
                }
            }
            else -> ty.displayName
        }
    }
}
