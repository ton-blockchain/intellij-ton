package org.ton.intellij.func.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.ton.intellij.func.psi.*


abstract class FuncReferenceExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), FuncReferenceExpression {

    override fun getReferences(): Array<FuncReference> {
        val parent = parent
        if (parent is FuncApplyExpression) {
            if (parent.right == this) {
                val left = parent.left
                if (left is FuncHoleTypeExpression || left is FuncPrimitiveTypeExpression) {
                    return EMPTY_ARRAY
                }
            }
        }

        return arrayOf(FuncReference(this, TextRange(0, textLength)))
    }

    override fun getReference(): FuncReference? = references.firstOrNull()

    override fun setName(name: String): PsiElement {
        identifier.replace(FuncPsiFactory[project].createIdentifierFromText(name))
        return this
    }

    override fun getTextOffset(): Int = identifier.textOffset

    override fun getName(): String? = identifier.text

    override fun getNameIdentifier(): PsiElement? = identifier

    companion object {
        private val EMPTY_ARRAY = emptyArray<FuncReference>()
    }
}
