package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.util.elementType
import org.ton.intellij.tolk.psi.TolkElementTypes
import org.ton.intellij.tolk.psi.TolkPrefixExpression
import org.ton.intellij.tolk.type.TolkTy

abstract class TolkPrefixExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkPrefixExpression {
    override val type: TolkTy?
        get() = if (firstChild.elementType == TolkElementTypes.EXCL) TolkTy.Bool else TolkTy.Int
}
