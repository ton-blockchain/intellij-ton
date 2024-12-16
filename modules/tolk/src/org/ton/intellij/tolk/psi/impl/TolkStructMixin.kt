package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.search.SearchScope
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tolk.psi.TolkStruct
import org.ton.intellij.tolk.stub.TolkStructStub

abstract class TolkStructMixin : TolkNamedElementImpl<TolkStructStub>, TolkStruct {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TolkStructStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun getUseScope(): SearchScope {
        return super.getUseScope()
    }
}
