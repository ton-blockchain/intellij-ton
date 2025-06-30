package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.util.parentOfType
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkSelfExpression
import org.ton.intellij.tolk.psi.TolkSelfParameter
import org.ton.intellij.tolk.psi.TolkTypeArgumentList
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.tolk.type.inference

abstract class TolkSelfExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkSelfExpression {
    override fun getReference() = TolkSelfExpressionReference(this)

    override val type: TolkTy?
        get() = inference?.getType(this)

    override val typeArgumentList: TolkTypeArgumentList?
        get() = null

    override val referenceNameElement: PsiElement?
        get() = selfKeyword

    override val referenceName: String? get() = "self"

    override fun toString(): String = "TolkSelfExpression"

    class TolkSelfExpressionReference(element: TolkSelfExpression) : PsiReferenceBase<TolkSelfExpression>(
        element
    ) {
        override fun resolve(): TolkSelfParameter? {
            val function = element.parentOfType<TolkFunction>() ?: return null
            return function.parameterList?.selfParameter
        }

        override fun calculateDefaultRangeInElement(): TextRange = element.selfKeyword.textRangeInParent
    }
}
