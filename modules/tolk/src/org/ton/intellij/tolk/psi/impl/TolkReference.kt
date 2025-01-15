package org.ton.intellij.tolk.psi.impl

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.ResolveCache
import org.ton.intellij.tolk.psi.TolkInferenceContextOwner
import org.ton.intellij.tolk.psi.TolkPsiFactory
import org.ton.intellij.tolk.psi.TolkReferenceExpression
import org.ton.intellij.tolk.psi.selfInferenceResult
import org.ton.intellij.util.parentOfType

class TolkReference(
    element: TolkReferenceExpression,
    rangeInElement: TextRange,
) : PsiReferenceBase.Poly<TolkReferenceExpression>(element, rangeInElement, false) {
    val identifier: PsiElement get() = element.identifier

    private val resolver = ResolveCache.PolyVariantResolver<TolkReference> { t, incompleteCode ->
        if (!myElement.isValid) return@PolyVariantResolver ResolveResult.EMPTY_ARRAY

        val inference = element.parentOfType<TolkInferenceContextOwner>()?.selfInferenceResult
        val inferenceResolved = inference?.getResolvedRefs(element)
        if (!inferenceResolved.isNullOrEmpty()) {
            return@PolyVariantResolver inferenceResolved.toTypedArray()
        }
        ResolveResult.EMPTY_ARRAY
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        if (!myElement.isValid) return ResolveResult.EMPTY_ARRAY
        return ResolveCache.getInstance(myElement.project).resolveWithCaching(this, resolver, false, incompleteCode)
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        return element.identifier.replace(TolkPsiFactory[element.project].createIdentifier(newElementName))
    }
}
