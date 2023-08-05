package org.ton.intellij.func.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.ton.intellij.func.psi.*

abstract class FuncReferenceExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), FuncReferenceExpression {

    override fun getReferences(): Array<FuncReference> {
//        val parent = parent
//        when (parent) {
//            is FuncVarExpression -> return emptyArray()
//        }
//        val grandParent = parent?.parent
//        when (grandParent) {
//            is FuncVarExpression,
//            is FuncConstVariable,
//            -> {
//                if (parent !is FuncAssignExpression || parent.expressionList.firstOrNull() == this) {
//                    return emptyArray()
//                }
//            }
//        }
        return PsiTreeUtil.treeWalkUp(this, null) { scope, prevParent ->
            when (scope) {
                is FuncCatch -> {
                    if (scope.expression == prevParent) return@treeWalkUp false
                }

                is FuncAssignExpression -> {
                    when (scope.parent) {
                        is FuncVarExpression -> {
                            if (scope.expressionList.firstOrNull() == prevParent) return@treeWalkUp false
                        }
                    }
                }

                is FuncVarExpression -> {
                    if (scope.expressionList.getOrNull(1) == prevParent) return@treeWalkUp false
                }
            }
            true
        }.let { result ->
            if (result) arrayOf(FuncReference(this, TextRange(0, textLength)))
            else emptyArray<FuncReference>()
        }
    }

    override fun getReference(): FuncReference? = references.firstOrNull()

    override fun setName(name: String): PsiElement {
        identifier.replace(FuncPsiFactory[project].createIdentifierFromText(name))
        return this
    }

    override fun getTextOffset(): Int = identifier.textOffset

    override fun getName(): String? = identifier.text

    override fun getNameIdentifier(): PsiElement? = identifier
}
