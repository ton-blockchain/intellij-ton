package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkTensorTypeExpression
import org.ton.intellij.tolk.type.TolkType

abstract class TolkTensorTypeExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkTensorTypeExpression {
    override val type: TolkType?
        get() = TolkType.tensor(typeExpressionList.map { it.type ?: return null })
}
