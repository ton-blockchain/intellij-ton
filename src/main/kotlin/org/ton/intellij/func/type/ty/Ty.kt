package org.ton.intellij.func.type.ty

import com.intellij.psi.util.elementType
import org.ton.intellij.func.psi.*
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
        FuncTy(types.map { it.foldWith(folder) })

    override fun isEquivalentToInner(other: FuncTy): Boolean {
        if (this === other) return true
        if (other is FuncTyUnit) {
            return types.size == 1 && types.single().isEquivalentTo(other)
        }
        if (other !is FuncTyTensor) return false
        if (types.size != other.types.size) return false
        for (i in types.indices) {
            if (!types[i].isEquivalentTo(other.types[i])) return false
        }

        return true
    }

    override fun toString(): String = types.joinToString(", ", "(", ")")
}

data class FuncTyTuple(
    val types: List<FuncTy>
) : FuncTy() {
    init {
        assert(types.isNotEmpty()) { "TyTuple should not be empty" }
    }

    override fun superFoldWith(folder: FuncTyFolder): FuncTy =
        FuncTyTuple(types.map { it.foldWith(folder) })

    override fun isEquivalentToInner(other: FuncTy): Boolean {
        if (this === other) return true
        if (other !is FuncTyTensor) return false

        if (types.size != other.types.size) return false
        for (i in types.indices) {
            if (!types[i].isEquivalentTo(other.types[i])) return false
        }

        return true
    }

    override fun toString(): String = types.joinToString(", ", "[", "]")
}

fun FuncTy(types: List<FuncTy>): FuncTy {
    return when (types.size) {
        0 -> FuncTyUnit
        1 -> types.single()
        else -> FuncTyTensor(types)
    }
}

data object FuncTyUnknown : FuncTy()

data class FuncTyMap(
    val from: FuncTy,
    val to: FuncTy
) : FuncTy() {
    override fun superFoldWith(folder: FuncTyFolder): FuncTy =
        FuncTyMap(from.foldWith(folder), to.foldWith(folder))

    override fun isEquivalentToInner(other: FuncTy): Boolean {
        if (this === other) return true
        if (other !is FuncTyMap) return false

        return from.isEquivalentTo(other.from) && to.isEquivalentTo(other.to)
    }

    override fun toString(): String = "($from) -> $to"
}

sealed class FuncTyAtomic : FuncTy() {
    abstract val name: String
}

data object FuncTyUnit : FuncTyAtomic() {
    override val name: String = "()"

    override fun isEquivalentToInner(other: FuncTy): Boolean {
        if (this === other) return true
        return other is FuncTyTensor && other.types.isEmpty()
    }
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

data object FuncTyAtomicTuple : FuncTyAtomic() {
    override val name: String = "tuple"
}

class FuncTyVar : FuncTy()

val FuncTypeReference.rawType: FuncTy
    get() {
        return when (this) {
            is FuncPrimitiveType -> when (firstChild.elementType) {
                FuncElementTypes.INT_KEYWORD -> FuncTyInt
                FuncElementTypes.CELL_KEYWORD -> FuncTyCell
                FuncElementTypes.SLICE_KEYWORD -> FuncTySlice
                FuncElementTypes.BUILDER_KEYWORD -> FuncTyBuilder
                FuncElementTypes.CONT_KEYWORD -> FuncTyCont
                FuncElementTypes.TUPLE_KEYWORD -> FuncTyAtomicTuple
                else -> FuncTyUnknown
            }

            is FuncUnitType -> FuncTyUnit
            is FuncTensorType -> FuncTy(
                typeReferenceList.map {
                    it.rawType
                }
            )

            is FuncTupleType -> FuncTyTuple(
                typeReferenceList.map {
                    it.rawType
                }
            )

            is FuncMapType -> FuncTyMap(
                from.rawType,
                to?.rawType ?: return FuncTyUnknown
            )

            else -> FuncTyUnknown
        }
    }
