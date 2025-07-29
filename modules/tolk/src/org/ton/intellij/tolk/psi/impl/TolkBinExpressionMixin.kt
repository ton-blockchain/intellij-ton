package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkBinExpression
import org.ton.intellij.tolk.psi.TolkElementTypes
import org.ton.intellij.util.tokenSetOf


abstract class TolkBinExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkBinExpression {
    override fun toString(): String = "TolkBinExpression:$text"
}

val TolkBinExpression.isSetAssignment
    get() = this.binaryOp.node.firstChildNode.elementType in ASSIGNMENT_OPERATORS

private val ASSIGNMENT_OPERATORS = tokenSetOf(
    TolkElementTypes.PLUSLET,
    TolkElementTypes.MINUSLET,
    TolkElementTypes.TIMESLET,
    TolkElementTypes.DIVLET,
    TolkElementTypes.DIVCLET,
    TolkElementTypes.DIVRLET,
    TolkElementTypes.MODLET,
    TolkElementTypes.MODRLET,
    TolkElementTypes.MODCLET,
    TolkElementTypes.LSHIFTLET,
    TolkElementTypes.RSHIFTCLET,
    TolkElementTypes.RSHIFTRLET,
    TolkElementTypes.RSHIFTLET,
    TolkElementTypes.ANDLET,
    TolkElementTypes.XORLET,
    TolkElementTypes.ORLET,
)
