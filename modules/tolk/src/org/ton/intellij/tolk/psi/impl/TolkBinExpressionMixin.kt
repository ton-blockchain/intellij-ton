package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkBinExpression


abstract class TolkBinExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkBinExpression {
    override fun toString(): String = "TolkBinExpression(\"$text\")"
}
