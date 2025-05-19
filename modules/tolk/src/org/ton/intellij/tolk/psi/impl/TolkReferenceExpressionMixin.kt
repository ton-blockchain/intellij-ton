package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.psi.TolkReferenceElement
import org.ton.intellij.tolk.psi.TolkReferenceExpression
import org.ton.intellij.tolk.psi.reference.TolkSymbolReference
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.tolk.type.inference


abstract class TolkReferenceExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkReferenceExpression, TolkReferenceElement {
    override val referenceNameElement: PsiElement?
        get() = identifier

    override fun getReferences(): Array<TolkSymbolReference> {
        val name = referenceName ?: return EMPTY_ARRAY
        if (TolkTy.byName(name) != null) return EMPTY_ARRAY
        return arrayOf(TolkSymbolReference(this))
    }

    override val type: TolkTy?
        get() {
            val inference = inference ?: return null
            val result = inference.getType(this)
            return result
        }

    override fun getReference(): TolkSymbolReference? = references.firstOrNull()

    override fun toString(): String = "TolkReferenceExpression:$text"

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
