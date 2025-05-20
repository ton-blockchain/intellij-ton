package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkTensorTypeExpression
import org.ton.intellij.tolk.type.TolkTy

abstract class TolkTensorTypeExpressionMixin : TolkTypeExpressionImpl, TolkTensorTypeExpression {

    constructor(node: ASTNode) : super(node)
//    constructor(stub: TolkReferenceTypeExpressionStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override val type: TolkTy?
        get() = TolkTy.tensor(typeExpressionList.map { it.type ?: return null })
}
