package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tolk.psi.TolkTensorTypeExpression
import org.ton.intellij.tolk.stub.TolkTypeStub
import org.ton.intellij.tolk.type.TolkTy

abstract class TolkTensorTypeExpressionMixin : TolkTypeExpressionImpl, TolkTensorTypeExpression {

    constructor(node: ASTNode) : super(node)
    constructor(stub: TolkTypeStub<*>, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override val type: TolkTy
        get() = TolkTy.tensor(typeExpressionList.map { it.type ?: TolkTy.Unknown })
}
