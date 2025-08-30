package org.ton.intellij.func.type.ty

import com.intellij.psi.util.elementType
import org.ton.intellij.func.psi.*
import org.ton.intellij.func.type.infer.FuncTyFoldable
import org.ton.intellij.func.type.infer.FuncTyFolder

interface FuncTyProvider {
    fun getFuncTy(): FuncTy
}

sealed class FuncTy : FuncTyFoldable<FuncTy>, FuncTyProvider {
    override fun getFuncTy(): FuncTy {
        return this
    }

    override fun foldWith(folder: FuncTyFolder): FuncTy = folder.foldTy(this)

    override fun superFoldWith(folder: FuncTyFolder): FuncTy = this

    fun isEquivalentTo(other: FuncTy?): Boolean = other != null && isEquivalentToInner(other)

    protected open fun isEquivalentToInner(other: FuncTy): Boolean = equals(other)

    open fun canRhsBeAssigned(other: FuncTy): Boolean {
        if (other is FuncTyUnknown) return true
        return this == other || this.isEquivalentToInner(other)
    }
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

    override fun canRhsBeAssigned(other: FuncTy): Boolean {
        if (other is FuncTyTensor) {
            if (types.size != other.types.size) return false
            for (i in types.indices) {
                if (!types[i].canRhsBeAssigned(other.types[i])) return false
            }
            return true
        }
        return super.canRhsBeAssigned(other)
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

    override fun canRhsBeAssigned(other: FuncTy): Boolean {
        if (other is FuncTyTuple) {
            if (types.size != other.types.size) return false
            for (i in types.indices) {
                if (!types[i].canRhsBeAssigned(other.types[i])) return false
            }
            return true
        }
        return super.canRhsBeAssigned(other)
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

data object FuncTyUnknown : FuncTy() {
    override fun toString() = "unknown"

    override fun canRhsBeAssigned(other: FuncTy): Boolean = true
}

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

    override fun canRhsBeAssigned(other: FuncTy): Boolean {
        if (other is FuncTyMap) {
            return from.canRhsBeAssigned(other.from) && to.canRhsBeAssigned(other.to)
        }
        return super.canRhsBeAssigned(other)
    }

    override fun toString(): String {
        if (from is FuncTyTensor || from is FuncTyUnit) {
            return "($from -> $to"
        }
        return "($from) -> $to"
    }
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

    override fun canRhsBeAssigned(other: FuncTy): Boolean {
        if (other is FuncTyUnit) {
            return true
        }
        return super.canRhsBeAssigned(other)
    }

    override fun toString(): String = "()"
}

data object FuncTyInt : FuncTyAtomic() {
    override val name: String = "int"

    override fun toString(): String = "int"
}

data object FuncTyCell : FuncTyAtomic() {
    override val name: String = "cell"

    override fun toString(): String = "cell"
}

data object FuncTySlice : FuncTyAtomic() {
    override val name: String = "slice"

    override fun toString(): String = "slice"
}

data object FuncTyBuilder : FuncTyAtomic() {
    override val name: String = "builder"

    override fun toString(): String = "builder"
}

data object FuncTyCont : FuncTyAtomic() {
    override val name: String = "cont"

    override fun toString(): String = "cont"
}

data object FuncTyAtomicTuple : FuncTyAtomic() {
    override val name: String = "tuple"

    override fun toString(): String = "tuple"
}

class FuncTyVar : FuncTy() {
    override fun canRhsBeAssigned(other: FuncTy): Boolean = true
}

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

val FuncPrimitiveTypeExpression.rawType: FuncTy
    get() {
        return when {
            this.primitiveType.builderKeyword != null -> FuncTyBuilder
            this.primitiveType.cellKeyword != null    -> FuncTyCell
            this.primitiveType.contKeyword != null    -> FuncTyCont
            this.primitiveType.intKeyword != null     -> FuncTyInt
            this.primitiveType.sliceKeyword != null   -> FuncTySlice
            this.primitiveType.tupleKeyword != null   -> FuncTyAtomicTuple
            else                                      -> FuncTyUnknown
        }
    }
