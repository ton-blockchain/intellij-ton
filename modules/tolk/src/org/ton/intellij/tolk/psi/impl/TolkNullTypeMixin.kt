package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tolk.psi.TolkNullTypeExpression
import org.ton.intellij.tolk.stub.TolkTypeStub
import org.ton.intellij.tolk.type.TolkTy

abstract class TolkNullTypeMixin : TolkTypeExpressionImpl, TolkNullTypeExpression {
    override val type: TolkTy get() = TolkTy.Null

    constructor(node: ASTNode) : super(node)
    constructor(stub: TolkTypeStub<*>, stubType: IStubElementType<*, *>) : super(stub, stubType)
}
