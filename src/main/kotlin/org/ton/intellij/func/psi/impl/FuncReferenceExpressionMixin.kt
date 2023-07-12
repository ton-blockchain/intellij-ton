package org.ton.intellij.func.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import org.ton.intellij.func.psi.FuncReferenceExpression
import org.ton.intellij.func.psi.FuncVarExpression

abstract class FuncReferenceExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), FuncReferenceExpression {

    override fun getReferences(): Array<PsiReference> {
        return if (parent !is FuncVarExpression) {
            arrayOf(
                FuncReference(this, TextRange(0, textLength))
            )
        } else {
            emptyArray()
        }
    }

    override fun setName(name: String): PsiElement {
        println("TODO: set name in func named element")
        return this
    }

    override fun getTextOffset(): Int = identifier.textOffset

    override fun getName(): String? = identifier.text

    override fun getNameIdentifier(): PsiElement? = identifier
}
