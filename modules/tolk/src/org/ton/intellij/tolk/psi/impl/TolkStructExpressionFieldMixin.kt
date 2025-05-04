package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.util.parentOfType
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.type.TyStruct

abstract class TolkStructExpressionFieldMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkStructExpressionField {
    override fun getReferences(): Array<out PsiReference> {
        return arrayOf(FieldReference(this))
    }

    class FieldReference(val field: TolkStructExpressionField) : PsiReferenceBase.Poly <TolkStructExpressionField>(field, field.identifier.textRangeInParent, false) {
        val identifier: PsiElement get() = element.identifier

        private val resolver = ResolveCache.PolyVariantResolver<FieldReference> { t, incompleteCode ->
            resolveInner(incompleteCode)
        }

        override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
            if (!myElement.isValid) return ResolveResult.EMPTY_ARRAY
            return resolver.resolve(this, incompleteCode)
//            return ResolveCache.getInstance(myElement.project).resolveWithCaching(this, resolver, false, incompleteCode)
        }

        override fun handleElementRename(newElementName: String): PsiElement {
            return identifier.replace(TolkPsiFactory[element.project].createIdentifier(newElementName))
        }

        private fun resolveInner(incompleteCode: Boolean): Array<ResolveResult> {
            if (field.node.findChildByType(TolkElementTypes.COLON) == null) {
                // resolve local variable
                val inference = element.parentOfType<TolkInferenceContextOwner>()?.selfInferenceResult ?: return ResolveResult.EMPTY_ARRAY
                val resolvedRefs = inference.getResolvedRefs(field)
                return resolvedRefs.toTypedArray()
            }
            val structExpression = field.parentOfType<TolkStructExpression>() ?: return ResolveResult.EMPTY_ARRAY

            val structType =
                structExpression.type?.unwrapTypeAlias() as? TyStruct ?: return ResolveResult.EMPTY_ARRAY

            val name = field.identifier.text.removeSurrounding("`")
            val field = structType.psi?.structBody?.structFieldList?.find {
                it.name == name
            } ?: return ResolveResult.EMPTY_ARRAY
            return arrayOf(PsiElementResolveResult(field))
        }
    }
}
