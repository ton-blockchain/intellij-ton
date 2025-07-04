package org.ton.intellij.tolk.psi.reference

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.util.parentOfType
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.type.TolkTyStruct

class TolkStructFieldReference(val field: TolkStructExpressionField) :
    PsiReferenceBase.Poly<TolkStructExpressionField>(field, field.identifier.textRangeInParent, false) {
    val identifier: PsiElement get() = element.identifier

    private val resolver = ResolveCache.PolyVariantResolver<TolkStructFieldReference> { t, incompleteCode ->
        resolveInner(incompleteCode)
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        if (!myElement.isValid) return ResolveResult.EMPTY_ARRAY
        return resolver.resolve(this, incompleteCode)
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val identifier = element.identifier
        val newId = TolkPsiFactory[identifier.project].createIdentifier(newElementName)
        identifier.replace(newId)
        return element
    }

    private fun resolveInner(incompleteCode: Boolean): Array<ResolveResult> {
        val resolvedRefs = if (field.node.findChildByType(TolkElementTypes.COLON) == null) {
            // resolve local variable
            val inference = element.parentOfType<TolkInferenceContextOwner>()?.selfInferenceResult
                ?: return ResolveResult.EMPTY_ARRAY
            inference.getResolvedRefs(field)
        } else emptyList()
        val structExpression = field.parentOfType<TolkStructExpression>() ?: return ResolveResult.EMPTY_ARRAY

        val structType =
            structExpression.type?.unwrapTypeAlias() as? TolkTyStruct ?: return ResolveResult.EMPTY_ARRAY

        val name = field.referenceName
        val field = structType.psi.structBody?.structFieldList?.find {
            it.name == name
        } ?: return ResolveResult.EMPTY_ARRAY
        return (resolvedRefs + PsiElementResolveResult(field)).toTypedArray()
    }
}
