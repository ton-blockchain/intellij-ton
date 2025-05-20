package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkParenTypeExpression
import org.ton.intellij.tolk.type.TolkTy

abstract class TolkParenTypeExpressionMixin(node: ASTNode) : TolkTypeExpressionImpl(node), TolkParenTypeExpression {
    override val type: TolkTy?
        get() = typeExpression?.type
}
