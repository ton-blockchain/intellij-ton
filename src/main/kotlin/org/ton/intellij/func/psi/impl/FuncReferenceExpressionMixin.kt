package org.ton.intellij.func.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.ton.intellij.func.psi.FuncAssignExpression
import org.ton.intellij.func.psi.FuncConstVariable
import org.ton.intellij.func.psi.FuncReferenceExpression
import org.ton.intellij.func.psi.FuncVarExpression

abstract class FuncReferenceExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), FuncReferenceExpression {

    override fun getReferences(): Array<FuncReference> {
        val parent = parent
        when (parent) {
            is FuncVarExpression -> return emptyArray()
        }
        val grandParent = parent?.parent
        when (grandParent) {
            is FuncVarExpression,
            is FuncConstVariable,
            -> {
                if (parent !is FuncAssignExpression || parent.expressionList.firstOrNull() == this) {
                    return emptyArray()
                }
            }
        }
        return arrayOf(
            FuncReference(this, TextRange(0, textLength))
        )
    }

    override fun getReference(): FuncReference? = references.firstOrNull()

    override fun setName(name: String): PsiElement {
        println("TODO: set name in func named element")
        return this
    }

    override fun getTextOffset(): Int = identifier.textOffset

    override fun getName(): String? = identifier.text

    override fun getNameIdentifier(): PsiElement? = identifier
}
