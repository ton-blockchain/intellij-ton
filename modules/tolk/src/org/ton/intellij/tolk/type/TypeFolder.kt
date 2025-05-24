package org.ton.intellij.tolk.type

interface TypeFolder {
    fun foldType(ty: TolkTy): TolkTy = ty
}

interface TypeFoldable<out Self> {
    fun foldWith(folder: TypeFolder): Self = superFoldWith(folder)

    fun superFoldWith(folder: TypeFolder): Self
}

fun <T : TypeFoldable<T>> T.substitute(substitution: Substitution): T =
    foldWith(object : TypeFolder {
        override fun foldType(ty: TolkTy): TolkTy = when {
            ty is TolkTypeParameterTy -> substitution[ty] ?: ty
            else -> ty.superFoldWith(this)
        }
    })
