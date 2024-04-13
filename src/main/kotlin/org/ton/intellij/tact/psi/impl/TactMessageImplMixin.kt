package org.ton.intellij.tact.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tact.psi.TactMessage
import org.ton.intellij.tact.psi.TactNamedElementImpl
import org.ton.intellij.tact.stub.TactMessageStub
import org.ton.intellij.tact.type.TactTy
import org.ton.intellij.tact.type.TactTyAdt

abstract class TactMessageImplMixin : TactNamedElementImpl<TactMessageStub>, TactMessage {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TactMessageStub, type: IStubElementType<*, *>) : super(stub, type)

    override val declaredType: TactTy
        get() = TactTyAdt(this)
}
