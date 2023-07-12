package org.ton.intellij.func.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.func.psi.FuncFunctionParameter
import org.ton.intellij.func.stub.FuncFunctionParameterStub

abstract class FuncFunctionParameterMixin : FuncNamedElementImpl<FuncFunctionParameterStub>, FuncFunctionParameter {
    constructor(node: ASTNode) : super(node)

    constructor(stub: FuncFunctionParameterStub, stubType: IStubElementType<*, *>) : super(stub, stubType)
}
