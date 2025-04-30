package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkGenericTypeExpression
import org.ton.intellij.tolk.type.TolkType

abstract class TolkGenericTypeExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkGenericTypeExpression {
    override val type: TolkType? get() = typeExpression.type
}
