package org.ton.intellij.tolk.type

interface TypeFolder {
    fun foldType(ty: TolkTy): TolkTy = ty
}

interface TypeFoldable<out Self> {
    fun foldWith(folder: TypeFolder): Self = superFoldWith(folder)

    fun superFoldWith(folder: TypeFolder): Self
}

fun <T : TypeFoldable<T>> TypeFoldable<T>.substitute(substitution: Substitution): T =
    foldWith(object : TypeFolder {
        override fun foldType(ty: TolkTy): TolkTy = when {
            ty is TyTypeParameter -> substitution[ty] ?: ty
            else -> ty.superFoldWith(this)
        }
    })


val substitution = mutableMapOf<TyTypeParameter, TolkTy>()
