package com.github.andreypfau.intellijton.func.resolve

import com.github.andreypfau.intellijton.func.psi.FuncElement
import com.github.andreypfau.intellijton.func.psi.FuncReferenceElement
import com.github.andreypfau.intellijton.func.psi.FuncTypes
import com.github.andreypfau.intellijton.func.psi.funcPsiFactory
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.elementType

interface FuncReference : PsiReference {
    override fun getElement(): FuncElement
    override fun resolve(): FuncElement?
    fun multiResolve(): Sequence<PsiElement>
}

abstract class FuncReferenceBase<T : FuncReferenceElement>(
    element: T
) : PsiPolyVariantReferenceBase<T>(element), FuncReference {
    override fun calculateDefaultRangeInElement() = TextRange(0, element.textRange.length)
    override fun getVariants(): Array<Any> = emptyArray()
    override fun resolve(): FuncElement? = super.resolve() as? FuncElement
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> =
        multiResolve().map(::PsiElementResolveResult).toList().toTypedArray()

    override fun handleElementRename(newElementName: String): PsiElement {
        doRename(element.referenceNameElement, newElementName)
        return element
    }
    protected open fun doRename(identifier: PsiElement, newName: String) {
        check(identifier.elementType == FuncTypes.IDENTIFIER)
        identifier.replace(identifier.project.funcPsiFactory.createIdentifier(newName.replace(".fc", "")))
    }
}
