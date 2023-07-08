package org.ton.intellij.func.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.func.psi.FuncElementTypes
import org.ton.intellij.func.psi.FuncFunction
import org.ton.intellij.func.stub.FuncFunctionStub

abstract class FuncFunctionMixin : FuncNamedElementImpl<FuncFunctionStub>, FuncFunction {
    constructor(node: ASTNode) : super(node)

    constructor(stub: FuncFunctionStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override val isImpure: Boolean
        get() = stub?.isImpure ?: (findChildByType<PsiElement>(FuncElementTypes.IMPURE_KEYWORD) != null)

    override val isMutable: Boolean
        get() = stub?.isMutable ?: (findChildByType<PsiElement>(FuncElementTypes.TILDE) != null)

    override val hasMethodId: Boolean
        get() = stub?.hasMethodId ?: (methodIdDefinition != null)
}
