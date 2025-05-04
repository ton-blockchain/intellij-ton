package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkParenTypeExpression
import org.ton.intellij.tolk.type.TolkTy

abstract class TolkParenTypeExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkParenTypeExpression {
    override val type: TolkTy?
        get() = typeExpression?.type
}
