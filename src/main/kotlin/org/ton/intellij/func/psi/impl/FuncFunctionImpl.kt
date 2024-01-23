package org.ton.intellij.func.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.func.psi.FuncElementTypes
import org.ton.intellij.func.psi.FuncFunction
import org.ton.intellij.func.stub.FuncFunctionStub

abstract class FuncFunctionMixin : FuncNamedElementImpl<FuncFunctionStub>, FuncFunction {
    constructor(node: ASTNode) : super(node)

    constructor(stub: FuncFunctionStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun toString(): String = "FuncFunction($containingFile - $name)"
}

val FuncFunction.isImpure: Boolean
    get() = stub?.isImpure ?: (node.findChildByType(FuncElementTypes.IMPURE_KEYWORD) != null)

val FuncFunction.isMutable: Boolean
    get() = stub?.isMutable ?: (node.findChildByType(FuncElementTypes.TILDE) != null)

val FuncFunction.hasMethodId: Boolean
    get() = stub?.hasMethodId ?: (methodIdDefinition != null)

val FuncFunction.hasAsm: Boolean
    get() = stub?.hasAsm ?: (asmDefinition != null)
