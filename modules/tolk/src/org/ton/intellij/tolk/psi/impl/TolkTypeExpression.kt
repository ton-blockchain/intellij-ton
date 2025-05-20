package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubBase
import org.ton.intellij.tolk.psi.TolkTypeExpression

abstract class TolkTypeExpressionMixin : TolkStubbedElementImpl<StubBase<*>>, TolkTypeExpression {
    constructor(node: ASTNode) : super(node)

    constructor(stub: StubBase<*>, nodeType: IStubElementType<*, *>) : super(stub, nodeType)
}
