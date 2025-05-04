package org.ton.intellij.tolk.psi.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.ResolveCache
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.util.parentOfType

class TolkSymbolReference(
    element: TolkElement,
) : PsiReferenceBase.Poly<TolkElement>(element) {
    val identifier: PsiElement get() = element.node.findChildByType(TolkElementTypes.IDENTIFIER)!!.psi

    override fun calculateDefaultRangeInElement(): TextRange? {
        val identifier = identifier
        return TextRange(identifier.startOffsetInParent, identifier.textLength)
    }

    private val resolver = ResolveCache.PolyVariantResolver<TolkSymbolReference> { t, incompleteCode ->
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
        return identifier.replace(TolkPsiFactory.Companion[element.project].createIdentifier(newElementName))
    }
}
