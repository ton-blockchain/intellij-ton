package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkIsExpression

abstract class TolkIsExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkIsExpression {
    override fun toString(): String = "TolkIsExpression:$text"
}
