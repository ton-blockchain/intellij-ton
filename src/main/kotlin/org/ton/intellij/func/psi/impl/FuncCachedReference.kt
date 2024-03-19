package org.ton.intellij.func.psi.impl

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.impl.source.resolve.ResolveCache.AbstractResolver
import com.intellij.util.ArrayUtil
import org.ton.intellij.func.psi.FuncPsiFactory

abstract class FuncCachedReference<T : PsiElement>(
    element: T,
) : PsiReferenceBase<T>(
    element, TextRange.from(0, element.textLength)
) {
    abstract val identifier: PsiElement

    override fun resolve(): PsiElement? =
        if (myElement.isValid) ResolveCache.getInstance(myElement.project)
            .resolveWithCaching(this, MY_RESOLVER, false, false)
        else null

    protected abstract fun resolveInner(): PsiElement?

    override fun handleElementRename(newElementName: String): PsiElement {
        myElement.replace(FuncPsiFactory[myElement.project].createIdentifier(newElementName))
        return myElement
    }

    override fun getVariants(): Array<Any> = ArrayUtil.EMPTY_OBJECT_ARRAY

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FuncCachedReference<*>) return false
        return element == other.element
    }

    override fun hashCode(): Int = element.hashCode()

    companion object {
        private val MY_RESOLVER = AbstractResolver<FuncCachedReference<*>, PsiElement> { r, _ ->
            r.resolveInner()
        }
    }
}
