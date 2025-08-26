package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tolk.psi.TolkNullableTypeExpression
import org.ton.intellij.tolk.stub.TolkTypeStub
import org.ton.intellij.tolk.type.TolkTy

abstract class TolkNullableTypeExpressionMixin : TolkTypeExpressionImpl, TolkNullableTypeExpression {
    override val type: TolkTy? get() = typeExpression.type?.nullable()

    constructor(node: ASTNode) : super(node)
    constructor(stub: TolkTypeStub<*>, stubType: IStubElementType<*, *>) : super(stub, stubType)
}
