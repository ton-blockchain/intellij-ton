package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkNullTypeExpression
import org.ton.intellij.tolk.type.TolkTy

abstract class TolkNullTypeMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkNullTypeExpression {
    override val type: TolkTy get() = TolkTy.Null
}
