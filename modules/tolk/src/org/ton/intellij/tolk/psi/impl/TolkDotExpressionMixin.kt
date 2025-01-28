package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkDotExpression
import org.ton.intellij.tolk.type.TolkType
import org.ton.intellij.tolk.type.infer.inference

abstract class TolkDotExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkDotExpression {
    override val type: TolkType? get() = inference?.getType(this)
}
