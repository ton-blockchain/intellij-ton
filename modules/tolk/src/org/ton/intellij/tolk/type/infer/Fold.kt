package org.ton.intellij.tolk.type.infer

import org.ton.intellij.tolk.type.ty.TolkTy

interface TolkTyFolder {
    fun foldTy(ty: TolkTy): TolkTy = ty
}

interface TolkTyFoldable<out Self> {
    fun foldWith(folder: TolkTyFolder): Self

    fun superFoldWith(folder: TolkTyFolder): Self
}
