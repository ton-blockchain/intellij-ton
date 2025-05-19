package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkDotExpression

abstract class TolkDotExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkDotExpression {
    val targetIndex: Int? get() {
        return fieldLookup?.integerLiteral?.text?.toIntOrNull()
    }

    override fun toString(): String = "TolkDotExpression:$text"
}

val TolkDotExpression.targetIndex get() = (this as? TolkDotExpressionMixin)?.targetIndex
