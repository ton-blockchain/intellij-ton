package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkParenType
import org.ton.intellij.tolk.type.TolkType

abstract class TolkParenTypeMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkParenType {
    override val type: TolkType?
        get() = typeExpression?.type
}