package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tolk.psi.TolkUnionTypeExpression
import org.ton.intellij.tolk.stub.TolkTypeStub
import org.ton.intellij.tolk.type.TolkTy

abstract class TolkUnionTypeExpressionMixin : TolkTypeExpressionImpl, TolkUnionTypeExpression {
    override val type: TolkTy
        get() {
            val types = typeExpressionList.map { it.type ?: TolkTy.Unknown  }
            val unionTypes = TolkTy.union(types)
            return unionTypes
        }

    constructor(node: ASTNode) : super(node)
    constructor(stub: TolkTypeStub<*>, stubType: IStubElementType<*, *>) : super(stub, stubType)
}
