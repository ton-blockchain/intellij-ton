package com.github.andreypfau.intellijton.fift.resolve

import com.github.andreypfau.intellijton.fift.psi.FiftElement
import com.github.andreypfau.intellijton.fift.psi.FiftOrdinaryWord
import com.github.andreypfau.intellijton.fift.psi.fiftPsiFactory
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*

interface FiftReference : PsiReference {
    override fun getElement(): FiftElement
    override fun resolve(): FiftElement?
    fun multiResolve(): Sequence<PsiElement>
}

abstract class FiftReferenceBase<T : FiftElement>(
    element: T
) : PsiPolyVariantReferenceBase<T>(element), FiftReference {
    override fun calculateDefaultRangeInElement() = TextRange(0, element.textRange.length)
    override fun getVariants(): Array<Any> = emptyArray()
    override fun resolve(): FiftElement? = super.resolve() as? FiftElement
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> =
        multiResolve().map(::PsiElementResolveResult).toList().toTypedArray()
}

class FiftOrdinaryWordReference(
    element: FiftOrdinaryWord
) : FiftReferenceBase<FiftOrdinaryWord>(element) {
    override fun multiResolve(): Sequence<PsiElement> {
        val name = element.text
        val file = element.resolveFile()
        return file.resolveAllWordDefStatements().filter {
            it.name == name
        }
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val newNameElement =
            requireNotNull(element.project.fiftPsiFactory.createFromText<FiftOrdinaryWord>(newElementName))
        element.replace(newNameElement)
        return newNameElement
    }
}