package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkFunType
import org.ton.intellij.tolk.type.TolkType

abstract class TolkFunTypeMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkFunType {
    override val type: TolkType?
        get() {
            val typeExpressions = typeExpressionList
            val left = typeExpressions.getOrNull(0)?.type ?: return null
            val right = typeExpressions.getOrNull(1)?.type ?: return null
            return TolkType.Function(left, right)
        }
}