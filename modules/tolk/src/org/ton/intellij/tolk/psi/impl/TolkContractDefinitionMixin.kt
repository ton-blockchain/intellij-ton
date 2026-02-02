package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.stub.TolkContractDefinitionStub

abstract class TolkContractDefinitionMixin : TolkNamedElementImpl<TolkContractDefinitionStub>, TolkContractDefinition, TolkInferenceContextOwner {
    constructor(node: ASTNode) : super(node)
    constructor(stub: TolkContractDefinitionStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)
}
