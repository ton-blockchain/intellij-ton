package org.ton.intellij.tact.resolve

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.*
import org.ton.intellij.tact.psi.TactElement

interface TactReference : PsiPolyVariantReference {
    override fun getElement(): TactElement

    override fun resolve(): TactElement?

    fun multiResolve(): List<TactElement>
}

abstract class TactReferenceBase<T : TactElement>(element: T) : PsiPolyVariantReferenceBase<T>(element), TactReference {
    override fun resolve(): TactElement? = super.resolve() as? TactElement

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return multiResolve().map(::PsiElementResolveResult).toTypedArray()
    }

    override fun getVariants(): Array<LookupElement> = LookupElement.EMPTY_ARRAY

    override fun equals(other: Any?): Boolean = other is TactReferenceBase<*> && element === other.element

    override fun hashCode(): Int = element.hashCode()
}
