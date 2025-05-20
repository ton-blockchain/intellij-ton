package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkNullableTypeExpression
import org.ton.intellij.tolk.type.TolkTy

abstract class TolkNullableTypeExpressionMixin(node: ASTNode) : TolkTypeExpressionImpl(node), TolkNullableTypeExpression {
    override val type: TolkTy? get() = typeExpression.type?.nullable()
}
