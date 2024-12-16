package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.util.elementType
import org.ton.intellij.tolk.psi.TolkBinExpression
import org.ton.intellij.tolk.psi.TolkElementTypes
import org.ton.intellij.tolk.type.TolkType
import org.ton.intellij.util.tokenSetOf

private val boolOperators = tokenSetOf(
    TolkElementTypes.ANDAND,
    TolkElementTypes.OROR,
    TolkElementTypes.EQEQ,
    TolkElementTypes.GEQ,
    TolkElementTypes.LEQ,
    TolkElementTypes.GT,
    TolkElementTypes.LT,
)

abstract class TolkBinExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkBinExpression {
    override val type: TolkType?
        get() = binaryOp.let {
            if (it.firstChild.elementType in boolOperators) TolkType.Bool else TolkType.Int
        }
}