package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkNullTypeExpression
import org.ton.intellij.tolk.type.TolkType

abstract class TolkNullTypeMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkNullTypeExpression {
    override val type: TolkType get() = TolkType.Null
}
