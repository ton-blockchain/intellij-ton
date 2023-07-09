package org.ton.intellij.func.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiReference
import org.ton.intellij.func.psi.FuncReferenceExpression

abstract class FuncReferenceExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), FuncReferenceExpression {
    override fun getReferences(): Array<PsiReference> = arrayOf(
        FuncReference(this, TextRange(0, textLength))
    )
}
