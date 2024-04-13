package org.ton.intellij.tact.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tact.psi.TactNamedElementImpl
import org.ton.intellij.tact.psi.TactContract
import org.ton.intellij.tact.stub.TactContractStub
import org.ton.intellij.tact.type.TactTy
import org.ton.intellij.tact.type.TactTyAdt

abstract class TactContractImplMixin : TactNamedElementImpl<TactContractStub>, TactContract {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TactContractStub, type: IStubElementType<*, *>) : super(stub, type)

    override val declaredType: TactTy
        get() = TactTyAdt(this)
}
