package org.ton.intellij.tact.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tact.psi.TactNamedElementImpl
import org.ton.intellij.tact.psi.TactStruct
import org.ton.intellij.tact.stub.TactStructStub
import org.ton.intellij.tact.type.TactTy
import org.ton.intellij.tact.type.TactTyAdt

abstract class TactStructImplMixin : TactNamedElementImpl<TactStructStub>, TactStruct {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TactStructStub, type: IStubElementType<*, *>) : super(stub, type)

    override val declaredType: TactTy get() = TactTyAdt(this)
}
