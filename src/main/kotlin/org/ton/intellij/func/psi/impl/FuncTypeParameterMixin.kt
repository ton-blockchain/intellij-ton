package org.ton.intellij.func.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.func.psi.FuncTypeParameter
import org.ton.intellij.func.stub.FuncTypeParameterStub

abstract class FuncTypeParameterMixin : FuncNamedElementImpl<FuncTypeParameterStub>, FuncTypeParameter {
    constructor(node: ASTNode) : super(node)

    constructor(stub: FuncTypeParameterStub, stubType: IStubElementType<*, *>) : super(stub, stubType)
}
