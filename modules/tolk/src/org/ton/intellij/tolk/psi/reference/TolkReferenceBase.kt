package org.ton.intellij.tolk.psi.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import org.ton.intellij.tolk.psi.TolkElement
import org.ton.intellij.tolk.psi.TolkPsiFactory
import org.ton.intellij.tolk.psi.TolkReferenceElement

abstract class TolkReferenceBase<T : TolkReferenceElement>(
    element: T
) : PsiPolyVariantReferenceBase<T>(element) {
    override fun resolve(): TolkElement? = super.resolve() as? TolkElement

    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult?> {
        if (!myElement.isValid) return ResolveResult.EMPTY_ARRAY
        val resolved = multiResolve()
        if (resolved.isEmpty()) {
            return ResolveResult.EMPTY_ARRAY
        }
        val result = arrayOfNulls<ResolveResult>(resolved.size)
        resolved.forEachIndexed { index, element ->
            result[index] = PsiElementResolveResult(element)
        }
        return result
    }

    override fun calculateDefaultRangeInElement(): TextRange? {
        val anchor = element.referenceNameElement ?: return TextRange.EMPTY_RANGE
        return TextRange(anchor.startOffsetInParent, anchor.textLength)
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val identifier = element.referenceNameElement ?: return super.handleElementRename(newElementName)
        val newId = TolkPsiFactory[identifier.project].createIdentifier(newElementName)
        identifier.replace(newId)
        return element
    }

    abstract fun multiResolve(): List<TolkElement>

    override fun equals(other: Any?): Boolean = other is TolkReferenceBase<*> && element == other.element

    override fun hashCode(): Int = element.hashCode()
}
