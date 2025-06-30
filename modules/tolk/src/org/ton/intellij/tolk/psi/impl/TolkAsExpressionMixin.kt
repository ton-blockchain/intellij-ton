package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkAsExpression
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.tolk.type.TolkTyParam

abstract class TolkAsExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkAsExpression {
    override val type: TolkTy?
        get() = typeExpression?.type?.let {
            ((it as? TolkTyParam)?.parameter as? TolkTyParam.NamedTypeParameter)?.psi?.defaultTypeParameter?.typeExpression?.type
                ?: it
        }

    override fun toString(): String = "TolkAsExpression:$text"
}
