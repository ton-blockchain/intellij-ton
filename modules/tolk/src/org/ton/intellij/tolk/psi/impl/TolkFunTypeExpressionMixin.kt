package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkFunTypeExpression
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.tolk.type.TolkTyFunction

abstract class TolkFunTypeExpressionMixin(node: ASTNode) : TolkTypeExpressionImpl(node), TolkFunTypeExpression {
    override val type: TolkTy?
        get() {
            val typeExpressions = typeExpressionList
            val left = typeExpressions.getOrNull(0)?.type ?: return null
            val right = typeExpressions.getOrNull(1)?.type ?: return null
            return TolkTyFunction(listOf(left), right)
        }
}
