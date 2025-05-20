package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkFunTypeExpression
import org.ton.intellij.tolk.type.TolkFunctionTy
import org.ton.intellij.tolk.type.TolkTy

abstract class TolkFunTypeExpressionMixin(node: ASTNode) : TolkTypeExpressionImpl(node), TolkFunTypeExpression {
    override val type: TolkTy?
        get() {
            val typeExpressions = typeExpressionList
            val left = typeExpressions.getOrNull(0)?.type ?: return null
            val right = typeExpressions.getOrNull(1)?.type ?: return null
            return TolkFunctionTy(left, right)
        }
}
