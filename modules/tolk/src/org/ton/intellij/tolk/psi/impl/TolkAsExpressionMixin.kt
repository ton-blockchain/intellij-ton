package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkAsExpression
import org.ton.intellij.tolk.type.TolkType

abstract class TolkAsExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkAsExpression {
    override val type: TolkType? get() = typeExpression?.type
}