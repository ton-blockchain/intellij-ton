package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tolk.psi.TolkTypeParameter
import org.ton.intellij.tolk.stub.TolkTypeParameterStub

abstract class TolkTypeParameterMixin : TolkNamedElementImpl<TolkTypeParameterStub>, TolkTypeParameter {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TolkTypeParameterStub, stubType: IStubElementType<*, *>) : super(stub, stubType)
}
