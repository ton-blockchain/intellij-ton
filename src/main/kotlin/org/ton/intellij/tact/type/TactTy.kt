package org.ton.intellij.tact.type

import org.ton.intellij.tact.psi.TactTypeDeclarationElement

sealed class TactTy {
    abstract override fun toString(): String
}

data object TactTyUnknown : TactTy() {
    override fun toString(): String {
        return "???"
    }
}

sealed class TactTyPrimitive : TactTy() {
    abstract val name: String

    override fun toString(): String = name
}

object TactTyInt : TactTyPrimitive() {
    override val name: String
        get() = "Int"
}

object TactTyBool : TactTyPrimitive() {
    override val name: String
        get() = "Bool"
}

object TactTyBuilder : TactTyPrimitive() {
    override val name: String
        get() = "Builder"
}

object TactTySlice : TactTyPrimitive() {
    override val name: String
        get() = "Slice"
}

object TactTyCell : TactTyPrimitive() {
    override val name: String
        get() = "Cell"
}

object TactTyAddress : TactTyPrimitive() {
    override val name: String
        get() = "Address"
}

object TactTyString : TactTyPrimitive() {
    override val name: String
        get() = "String"
}

object TactTyStringBuilder : TactTyPrimitive() {
    override val name: String
        get() = "StringBuilder"
}


data class TactTyAdt(
    val item: TactTypeDeclarationElement
) : TactTy() {
    override fun toString(): String = item.name ?: item.toString()
}

data class TactTyNullable(
    val inner: TactTy
) : TactTy() {
    override fun toString(): String = "$inner?"
}
