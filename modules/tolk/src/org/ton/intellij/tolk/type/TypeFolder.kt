package org.ton.intellij.tolk.type

interface TypeFolder {
    fun foldType(ty: TolkTy): TolkTy = ty
}

interface TypeFoldable<out Self> {
    fun foldWith(folder: TypeFolder): Self = superFoldWith(folder)

    fun superFoldWith(folder: TypeFolder): Self
}

fun <T : TypeFoldable<T>> T.substitute(substitution: Substitution): T {
    if (substitution.isEmpty()) return this
    return foldWith(object : TypeFolder {
        override fun foldType(ty: TolkTy): TolkTy = when {
            ty is TolkTyParam -> substitution[ty] ?: ty
            else -> ty.superFoldWith(this)
        }
    })
}

fun <T : TypeFoldable<T>> T.unwrapTypeAliasDeeply(): T {
    return foldWith(object : TypeFolder {
        override fun foldType(ty: TolkTy): TolkTy = when (ty) {
            is TolkTyAlias -> ty.underlyingType
            else -> ty.superFoldWith(this)
        }
    })
}
