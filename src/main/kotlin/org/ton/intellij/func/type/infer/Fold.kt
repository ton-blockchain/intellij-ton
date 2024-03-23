package org.ton.intellij.func.type.infer

import org.ton.intellij.func.type.ty.FuncTy

interface FuncTyFolder {
    fun foldTy(ty: FuncTy): FuncTy = ty
}

interface FuncTyFoldable<out Self> {
    fun foldWith(folder: FuncTyFolder): Self

    fun superFoldWith(folder: FuncTyFolder): Self
}
