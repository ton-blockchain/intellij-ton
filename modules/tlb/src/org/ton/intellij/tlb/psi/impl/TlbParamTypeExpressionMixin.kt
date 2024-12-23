package org.ton.intellij.tlb.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import org.ton.intellij.tlb.psi.TlbParamTypeExpression
import org.ton.intellij.tlb.psi.TlbReference

abstract class TlbParamTypeExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TlbParamTypeExpression {
    override fun getReference(): PsiReference = TlbReference(this, textRange)
}