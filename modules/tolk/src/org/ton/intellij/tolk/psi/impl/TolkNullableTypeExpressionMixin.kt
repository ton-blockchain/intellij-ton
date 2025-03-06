package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkNullableTypeExpression
import org.ton.intellij.tolk.type.TolkType

abstract class TolkNullableTypeExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkNullableTypeExpression {
    override val type: TolkType? get() = typeExpression.type?.nullable()
}
