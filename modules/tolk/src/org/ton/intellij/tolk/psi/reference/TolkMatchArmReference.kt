package org.ton.intellij.tolk.psi.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import org.ton.intellij.tolk.psi.TolkMatchPatternReference
import org.ton.intellij.tolk.psi.TolkPsiFactory

class TolkMatchArmReference(
    element: TolkMatchPatternReference
) : PsiPolyVariantReferenceBase<TolkMatchPatternReference>(element) {
    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult?> {
        val typeResult = TolkTypeReference(element).multiResolve(incompleteCode)
        if (typeResult.isNotEmpty()) return typeResult

        val symbolResult = TolkSymbolReference(element).multiResolve(incompleteCode)
        if (symbolResult.isNotEmpty()) return symbolResult

        return ResolveResult.EMPTY_ARRAY
    }

    override fun calculateDefaultRangeInElement(): TextRange {
        val identifier = element.identifier
        return TextRange(identifier.startOffsetInParent, identifier.textLength)
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val identifier = element.identifier
        val newId = TolkPsiFactory[identifier.project].createIdentifier(newElementName)
        identifier.replace(newId)
        return element
    }
}
