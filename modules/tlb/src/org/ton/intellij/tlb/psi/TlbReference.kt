package org.ton.intellij.tlb.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.util.parentsOfType

class TlbReference(
    element: TlbParamTypeExpression,
    rangeInElement: TextRange,
) : PsiReferenceBase.Poly<TlbParamTypeExpression>(element, rangeInElement, false) {
    private val resolver = ResolveCache.PolyVariantResolver<TlbReference> { t, incompleteCode ->
        if (!myElement.isValid) return@PolyVariantResolver ResolveResult.EMPTY_ARRAY
        val name = t.element.identifier?.text ?: return@PolyVariantResolver ResolveResult.EMPTY_ARRAY
        t.element.parentsOfType<TlbFieldListOwner>().forEach {
            val field = it.fieldList?.fieldList?.find { field ->
                (field as? TlbNamedElement)?.name == name
            }
            if (field != null) {
                return@PolyVariantResolver arrayOf(PsiElementResolveResult(field))
            }
        }

        ResolveResult.EMPTY_ARRAY
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        if (!myElement.isValid) return ResolveResult.EMPTY_ARRAY
        return ResolveCache.getInstance(myElement.project).resolveWithCaching(this, resolver, false, incompleteCode)
    }
}