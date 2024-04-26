package org.ton.intellij.tact.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import org.ton.intellij.tact.psi.TactCallExpression
import org.ton.intellij.tact.resolve.TactFieldReference

abstract class TactCallExpressionImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), TactCallExpression {
    override fun getReference(): PsiReference {
        return TactFieldReference(this, identifier.textRangeInParent)
    }
}
