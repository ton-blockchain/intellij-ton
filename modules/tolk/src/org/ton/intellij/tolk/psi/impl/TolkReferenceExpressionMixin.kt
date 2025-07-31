package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.psi.TolkDotExpression
import org.ton.intellij.tolk.psi.TolkReferenceElement
import org.ton.intellij.tolk.psi.TolkReferenceExpression
import org.ton.intellij.tolk.psi.TolkTypeArgumentList
import org.ton.intellij.tolk.psi.reference.TolkSymbolReference
import org.ton.intellij.tolk.type.TolkPrimitiveTy
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.tolk.type.inference


abstract class TolkReferenceExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkReferenceExpression,
    TolkReferenceElement {
    override val typeArgumentList: TolkTypeArgumentList?
        get() = findChildByClass(TolkTypeArgumentList::class.java)

    override val referenceNameElement: PsiElement?
        get() = identifier

    override fun getReferences(): Array<TolkSymbolReference> {
        val name = referenceName ?: return EMPTY_ARRAY
        if (name == "_") return EMPTY_ARRAY
        val reference = TolkSymbolReference(this)
        if (reference.resolve() != null) {
            return arrayOf(reference)
        }
        if (parent is TolkDotExpression && TolkPrimitiveTy.fromName(name) != null) return EMPTY_ARRAY
        return arrayOf(reference)
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
