package org.ton.intellij.tact.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import org.ton.intellij.tact.psi.TactReferenceExpression
import org.ton.intellij.tact.resolve.TactFieldReference

abstract class TactReferenceExpressionImplMixin(
    node: ASTNode
) : ASTWrapperPsiElement(node), TactReferenceExpression {
    override fun getReference(): PsiReference {
        return TactFieldReference(this, identifier.textRangeInParent)
    }
}
