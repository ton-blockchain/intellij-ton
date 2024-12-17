package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.psi.TolkPsiFactory
import org.ton.intellij.tolk.psi.TolkReferenceExpression
import org.ton.intellij.tolk.psi.TolkTypedElement
import org.ton.intellij.tolk.type.TolkType


abstract class TolkReferenceExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkReferenceExpression {

    override val type: TolkType?
        get() = references.firstOrNull()?.resolve()?.let { it as? TolkTypedElement }?.type

    override fun getReferences(): Array<TolkReference> {
//        if (isVariableDefinition()) return EMPTY_ARRAY
        return arrayOf(TolkReference(this, TextRange(0, textLength)))
    }

    override fun getReference(): TolkReference? = references.firstOrNull()

    override fun setName(name: String): PsiElement {
        identifier.replace(TolkPsiFactory[project].createIdentifier(name))
        return this
    }

    override fun getTextOffset(): Int = identifier.textOffset

    override fun getName(): String? = identifier.text

    override fun getNameIdentifier(): PsiElement? = identifier

    companion object {
        private val EMPTY_ARRAY = emptyArray<TolkReference>()
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
