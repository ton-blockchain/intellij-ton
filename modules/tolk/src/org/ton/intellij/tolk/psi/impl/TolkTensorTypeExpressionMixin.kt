package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkTensorTypeExpression
import org.ton.intellij.tolk.type.TolkTy

abstract class TolkTensorTypeExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkTensorTypeExpression {
    override val type: TolkTy?
        get() = TolkTy.tensor(typeExpressionList.map { it.type ?: return null })
}
