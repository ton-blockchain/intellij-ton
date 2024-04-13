package org.ton.intellij.tact.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tact.psi.TactNamedElementImpl
import org.ton.intellij.tact.psi.TactPrimitive
import org.ton.intellij.tact.stub.TactPrimitiveStub
import org.ton.intellij.tact.type.*

abstract class TactPrimitiveImplMixin : TactNamedElementImpl<TactPrimitiveStub>, TactPrimitive {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TactPrimitiveStub, type: IStubElementType<*, *>) : super(stub, type)

    override val declaredType
        get() = when (name) {
            "Int" -> TactTyInt
            "Bool" -> TactTyBool
            "Builder" -> TactTyBuilder
            "Slice" -> TactTySlice
            "Cell" -> TactTyCell
            "Address" -> TactTyAddress
            "String" -> TactTyString
            "StringBuilder" -> TactTyStringBuilder
            else -> TactTyUnknown
        }
}
