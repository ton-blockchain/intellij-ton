package org.ton.intellij.func.type.ty

import org.ton.intellij.func.type.infer.FuncTyFoldable
import org.ton.intellij.func.type.infer.FuncTyFolder

sealed class FuncTy : FuncTyFoldable<FuncTy> {
    override fun foldWith(folder: FuncTyFolder): FuncTy = folder.foldTy(this)

    override fun superFoldWith(folder: FuncTyFolder): FuncTy = this

    fun isEquivalentTo(other: FuncTy?): Boolean = other != null && isEquivalentToInner(other)

    protected open fun isEquivalentToInner(other: FuncTy): Boolean = equals(other)
}

data class FuncTyTensor(
    val types: List<FuncTy>
) : FuncTy() {
    constructor(vararg types: FuncTy) : this(types.toList())

    init {
        assert(types.isNotEmpty()) { "TyTuple should not be empty" }
    }

    override fun superFoldWith(folder: FuncTyFolder): FuncTy =
        FuncTyTensor(types.map { it.foldWith(folder) })

    override fun isEquivalentToInner(other: FuncTy): Boolean {
        if (this === other) return true
        if (other !is FuncTyTensor) return false

        if (types.size != other.types.size) return false
        for (i in types.indices) {
            if (!types[i].isEquivalentTo(other.types[i])) return false
        }

        return true
    }
}

data object FuncTyUnknown : FuncTy()

sealed class FuncTyAtomic : FuncTy() {
    abstract val name: String
}

data object FuncTyUnit : FuncTyAtomic() {
    override val name: String = "()"
}

data object FuncTyInt : FuncTyAtomic() {
    override val name: String = "int"
}

data object FuncTyCell : FuncTyAtomic() {
    override val name: String = "cell"
}

data object FuncTySlice : FuncTyAtomic() {
    override val name: String = "slice"
}

data object FuncTyBuilder : FuncTyAtomic() {
    override val name: String = "builder"
}

data object FuncTyCont : FuncTyAtomic() {
    override val name: String = "cont"
}

class FuncTyVar : FuncTy()
