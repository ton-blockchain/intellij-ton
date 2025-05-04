package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.eval.TolkIntValue
import org.ton.intellij.tolk.eval.value
import org.ton.intellij.tolk.psi.TolkDotExpression
import org.ton.intellij.tolk.psi.TolkLiteralExpression
import org.ton.intellij.tolk.type.TolkTy

abstract class TolkDotExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkDotExpression {
    override val type: TolkTy? get() = right?.type

    val targetIndex: Int? get() = ((right as? TolkLiteralExpression)?.value as? TolkIntValue)?.value?.toInt()
}

val TolkDotExpression.targetIndex get() = (this as? TolkDotExpressionMixin)?.targetIndex
