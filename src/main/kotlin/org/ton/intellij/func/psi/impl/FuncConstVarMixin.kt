package org.ton.intellij.func.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.func.psi.FuncConstVar
import org.ton.intellij.func.stub.FuncConstVarStub

abstract class FuncConstVarMixin : FuncNamedElementImpl<FuncConstVarStub>, FuncConstVar {
    constructor(node: ASTNode) : super(node)

    constructor(stub: FuncConstVarStub, stubType: IStubElementType<*, *>) : super(stub, stubType)
}
