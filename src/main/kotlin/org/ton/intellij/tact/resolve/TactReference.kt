package org.ton.intellij.tact.resolve

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.ResolveCache
import org.apache.xml.resolver.apps.resolver
import org.ton.intellij.func.psi.impl.FuncReference
import org.ton.intellij.tact.psi.TactElement

interface TactReference : PsiPolyVariantReference {
    override fun getElement(): TactElement

    override fun resolve(): TactElement?

    fun multiResolve(): Collection<TactElement>
}

abstract class TactReferenceBase<T : TactElement>(element: T, range: TextRange) :
    PsiPolyVariantReferenceBase<T>(element, range), TactReference {
    private val resolver = ResolveCache.PolyVariantResolver<TactReferenceBase<T>> { t, incompleteCode ->
        if (!myElement.isValid) return@PolyVariantResolver ResolveResult.EMPTY_ARRAY
        multiResolve().map(::PsiElementResolveResult).toTypedArray()
    }

    override fun resolve(): TactElement? = super.resolve() as? TactElement

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        if (!myElement.isValid) return ResolveResult.EMPTY_ARRAY
        return ResolveCache.getInstance(myElement.project).resolveWithCaching(this, resolver, false, incompleteCode)
    }

    override fun getVariants(): Array<LookupElement> = LookupElement.EMPTY_ARRAY

    override fun equals(other: Any?): Boolean = other is TactReferenceBase<*> && element === other.element

    override fun hashCode(): Int = element.hashCode()
}
