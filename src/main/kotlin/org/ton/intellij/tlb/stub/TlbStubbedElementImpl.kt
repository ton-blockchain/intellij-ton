package org.ton.intellij.tlb.stub

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubBase
import org.ton.intellij.tlb.psi.TlbElement

abstract class TlbStubbedElementImpl<T : StubBase<*>> : StubBasedPsiElementBase<T>, TlbElement {
    constructor(stub: T, nodeType: IStubElementType<*, *>) : super(stub, nodeType)
    constructor(node: ASTNode) : super(node)

    override fun toString(): String = elementType.toString()
}
