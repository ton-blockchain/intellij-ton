package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.util.elementType
import org.ton.intellij.tolk.psi.TolkElementTypes
import org.ton.intellij.tolk.psi.TolkPrefixExpression
import org.ton.intellij.tolk.type.TolkType

abstract class TolkPrefixExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkPrefixExpression {
    override val type: TolkType?
        get() = if (firstChild.elementType == TolkElementTypes.EXCL) TolkType.Bool else TolkType.Int
}