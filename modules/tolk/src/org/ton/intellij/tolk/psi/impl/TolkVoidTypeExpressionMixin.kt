package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkVoidTypeExpression
import org.ton.intellij.tolk.type.TolkTy

abstract class TolkVoidTypeExpressionMixin(node: ASTNode) : TolkTypeExpressionImpl(node), TolkVoidTypeExpression {
    override val type: TolkTy get() = TolkTy.Unit
}
