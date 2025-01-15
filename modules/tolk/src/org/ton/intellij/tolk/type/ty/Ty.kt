package org.ton.intellij.tolk.type.ty

import org.ton.intellij.tolk.type.infer.TolkTyFoldable
import org.ton.intellij.tolk.type.infer.TolkTyFolder

interface TolkTyProvider {
    fun getTolkTy(): TolkTy
}

sealed class TolkTy : TolkTyFoldable<TolkTy>, TolkTyProvider {
    override fun getTolkTy(): TolkTy {
        return this
    }

    override fun foldWith(folder: TolkTyFolder): TolkTy = folder.foldTy(this)

    override fun superFoldWith(folder: TolkTyFolder): TolkTy = this

    fun isEquivalentTo(other: TolkTy?): Boolean = other != null && isEquivalentToInner(other)

    protected open fun isEquivalentToInner(other: TolkTy): Boolean = equals(other)
}

data class TolkTyTensor(
    val types: List<TolkTy>
) : TolkTy() {
    constructor(vararg types: TolkTy) : this(types.toList())

    init {
        assert(types.isNotEmpty()) { "TyTuple should not be empty" }
    }

    override fun superFoldWith(folder: TolkTyFolder): TolkTy =
        TolkTy(types.map { it.foldWith(folder) })

    override fun isEquivalentToInner(other: TolkTy): Boolean {
        if (this === other) return true
        if (other is TolkTyUnit) {
            return types.size == 1 && types.single().isEquivalentTo(other)
        }
        if (other !is TolkTyTensor) return false
        if (types.size != other.types.size) return false
        for (i in types.indices) {
            if (!types[i].isEquivalentTo(other.types[i])) return false
        }

        return true
    }

    override fun toString(): String = types.joinToString(", ", "(", ")")
}

@Deprecated("Use type.TolkTy instead")
data class TolkTyTuple(
    val types: List<TolkTy>
) : TolkTy() {
    init {
        assert(types.isNotEmpty()) { "TyTuple should not be empty" }
    }

    override fun superFoldWith(folder: TolkTyFolder): TolkTy =
        TolkTyTuple(types.map { it.foldWith(folder) })

    override fun isEquivalentToInner(other: TolkTy): Boolean {
        if (this === other) return true
        if (other !is TolkTyTensor) return false

        if (types.size != other.types.size) return false
        for (i in types.indices) {
            if (!types[i].isEquivalentTo(other.types[i])) return false
        }

        return true
    }

    override fun toString(): String = types.joinToString(", ", "[", "]")
}

fun TolkTy(types: List<TolkTy>): TolkTy {
    return when (types.size) {
        0 -> TolkTyUnit
        1 -> types.single()
        else -> TolkTyTensor(types)
    }
}

data object TolkTyUnknown : TolkTy() {
    override fun toString(): String {
        return "???"
    }
}

data class TolkTyMap(
    val from: TolkTy,
    val to: TolkTy
) : TolkTy() {
    override fun superFoldWith(folder: TolkTyFolder): TolkTy =
        TolkTyMap(from.foldWith(folder), to.foldWith(folder))

    override fun isEquivalentToInner(other: TolkTy): Boolean {
        if (this === other) return true
        if (other !is TolkTyMap) return false

        return from.isEquivalentTo(other.from) && to.isEquivalentTo(other.to)
    }

    override fun toString(): String = "($from) -> $to"
}

sealed class TolkTyAtomic : TolkTy() {
    abstract val name: String
}

data object TolkTyUnit : TolkTyAtomic() {
    override val name: String = "()"

    override fun isEquivalentToInner(other: TolkTy): Boolean {
        if (this === other) return true
        return other is TolkTyTensor && other.types.isEmpty()
    }

    override fun toString(): String = "()"
}
