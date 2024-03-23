package org.ton.intellij.func.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider.Result
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import org.ton.intellij.func.psi.*


abstract class FuncReferenceExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), FuncReferenceExpression {

    override fun getReferences(): Array<FuncReference> {
        if (isVariableDefinition()) return EMPTY_ARRAY
        return arrayOf(FuncReference(this, TextRange(0, textLength)))
    }

    override fun getReference(): FuncReference? = references.firstOrNull()

    override fun setName(name: String): PsiElement {
        identifier.replace(FuncPsiFactory[project].createIdentifier(name))
        return this
    }

    override fun getTextOffset(): Int = identifier.textOffset

    override fun getName(): String? = identifier.text

    override fun getNameIdentifier(): PsiElement? = identifier

    companion object {
        private val EMPTY_ARRAY = emptyArray<FuncReference>()
    }
}

private fun FuncExpression.isTypeExpression(): Boolean =
    when (this) {
        is FuncTensorExpression -> this.expressionList.all { it.isTypeExpression() }
        is FuncTupleExpression -> this.expressionList.all { it.isTypeExpression() }
        is FuncHoleTypeExpression,
        is FuncPrimitiveTypeExpression -> true

        else -> false
    }

fun FuncReferenceExpression.isVariableDefinition(): Boolean = CachedValuesManager.getCachedValue(this) {
    val result = !PsiTreeUtil.treeWalkUp(this, null) { scope, lastParent ->
        if (scope is FuncApplyExpression && scope.right == lastParent) { // `var |foo|` <-- last parent
            val left = scope.left // type definition -> `|var| foo`
            if (left.isTypeExpression()) {
                return@treeWalkUp false
            }
        }
        true
    }
    Result(result, this)
}
