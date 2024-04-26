package org.ton.intellij.tact.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import org.ton.intellij.tact.psi.TactStructExpression
import org.ton.intellij.tact.resolve.TactTypeReference

abstract class TactStructExpressionImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), TactStructExpression {
    override fun getReference(): PsiReference {
        return TactTypeReference(this, identifier.textRangeInParent)
    }
}
