package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkNullTypeExpression
import org.ton.intellij.tolk.type.TolkTy

abstract class TolkNullTypeMixin(node: ASTNode) : TolkTypeExpressionImpl(node), TolkNullTypeExpression {
    override val type: TolkTy get() = TolkTy.Null
}
