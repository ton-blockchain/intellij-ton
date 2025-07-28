package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkCallExpression
import org.ton.intellij.tolk.psi.TolkDotExpression
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkReferenceExpression
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.tolk.type.inference

val TolkCallExpression.functionSymbol: TolkFunction?
    get() = inference?.getResolvedFunction(this)

abstract class TolkCallExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkCallExpression {
    override val type: TolkTy? get() = inference?.getType(this)

    override fun toString(): String = "TolkCallExpression:$text"
}
