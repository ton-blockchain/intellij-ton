package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.util.parentOfType
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkSelfTypeExpression
import org.ton.intellij.tolk.psi.TolkTypeExpression
import org.ton.intellij.tolk.type.TolkTy

abstract class TolkSelfTypeExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkSelfTypeExpression {
    override val type: TolkTy
        get() = reference.resolve()?.type ?: TolkTy.Unknown

    override fun getReference() = SelfTypeReference(this)

    class SelfTypeReference(
        element: TolkSelfTypeExpression
    ) : PsiReferenceBase<TolkSelfTypeExpression>(
        element, TextRange(0, element.textLength), false
    ) {
        override fun resolve(): TolkTypeExpression? {
            val function = element.parentOfType<TolkFunction>()
            return function?.functionReceiver?.typeExpression
        }
    }
}
