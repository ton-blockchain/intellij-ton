package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkDotExpression
import org.ton.intellij.tolk.type.TolkType

abstract class TolkDotExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkDotExpression {
    override val type: TolkType? get() = right?.type
}
