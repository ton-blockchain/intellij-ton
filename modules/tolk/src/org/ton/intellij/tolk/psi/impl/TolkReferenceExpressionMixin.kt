package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.psi.TolkPsiFactory
import org.ton.intellij.tolk.psi.TolkReferenceExpression
import org.ton.intellij.tolk.psi.reference.TolkSymbolReference
import org.ton.intellij.tolk.type.TolkTy


abstract class TolkReferenceExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkReferenceExpression {
    override fun getReferences(): Array<TolkSymbolReference> {
        val name = name
        if (name == "__expect_type") return EMPTY_ARRAY
        if (name != null && TolkTy.byName(name) != null) return EMPTY_ARRAY
        return arrayOf(TolkSymbolReference(this))
    }

    override fun getReference(): TolkSymbolReference? = references.firstOrNull()

    override fun setName(name: String): PsiElement {
        identifier.replace(TolkPsiFactory[project].createIdentifier(name))
        return this
    }

    override fun getTextOffset(): Int = identifier.textOffset

    override fun getName(): String? = identifier.text.removeSurrounding("`")

    override fun getNameIdentifier(): PsiElement? = identifier

    override fun toString(): String = "TolkReferenceExpression($text)"

    companion object {
        private val EMPTY_ARRAY = emptyArray<TolkSymbolReference>()
    }
}

//fun TolkReferenceExpression.isVariableDefinition(): Boolean = CachedValuesManager.getCachedValue(this) {
//    val result = !PsiTreeUtil.treeWalkUp(this, null) { scope, lastParent ->
//        if (scope is TolkApplyExpression && scope.right == lastParent) { // `var |foo|` <-- last parent
//            val left = scope.left // type definition -> `|var| foo`
//            if (left.isTypeExpression()) {
//                return@treeWalkUp false
//            }
//        }
//        if (scope is TolkCatch && lastParent in scope.referenceExpressionList) {
//            return@treeWalkUp false
//        }
//        true
//    }
//    Result(result, this)
//}
