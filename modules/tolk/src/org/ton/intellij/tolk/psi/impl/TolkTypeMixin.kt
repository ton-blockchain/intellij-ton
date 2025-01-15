package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.search.SearchScope
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tolk.psi.TolkTypeDef
import org.ton.intellij.tolk.stub.TolkTypeDefStub

abstract class TolkTypeMixin : TolkNamedElementImpl<TolkTypeDefStub>, TolkTypeDef {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TolkTypeDefStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun getUseScope(): SearchScope {
        return super.getUseScope()
    }


}
