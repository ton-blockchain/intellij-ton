package org.ton.intellij.tact.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import org.ton.intellij.tact.psi.TactInitOfExpression
import org.ton.intellij.tact.resolve.TactTypeReference

abstract class TactInitOfExpressionImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), TactInitOfExpression {
    override fun getReference(): PsiReference? {
        return TactTypeReference(this, identifier?.textRangeInParent ?: return null)
    }
}
