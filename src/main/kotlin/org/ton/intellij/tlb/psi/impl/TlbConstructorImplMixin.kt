package org.ton.intellij.tlb.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tlb.psi.TlbConstructor
import org.ton.intellij.tlb.psi.TlbNamedElementImpl
import org.ton.intellij.tlb.stub.TlbConstructorStub

abstract class TlbConstructorImplMixin : TlbNamedElementImpl<TlbConstructorStub>, TlbConstructor {
    constructor(node: ASTNode) : super(node)
    constructor(stub: TlbConstructorStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun toString(): String {
        return "TlbConstructor($containingFile - $name)"
    }
}
