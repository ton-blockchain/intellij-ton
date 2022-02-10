package com.github.andreypfau.intellijton.func.resolve


import com.github.andreypfau.intellijton.func.psi.FuncElement
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*

interface FuncReference : PsiReference {
    override fun getElement(): FuncElement
    override fun resolve(): FuncElement?
    fun multiResolve(): Sequence<PsiElement>
}

abstract class FuncReferenceImpl<T : FuncElement>(
    element: T
) : PsiPolyVariantReferenceBase<T>(element), FuncReference {
    override fun calculateDefaultRangeInElement() = TextRange(0, element.textRange.length)
    override fun getVariants(): Array<Any> = emptyArray()
    override fun resolve(): FuncElement? = super.resolve() as? FuncElement
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> =
        multiResolve().map(::PsiElementResolveResult).toList().toTypedArray()
}
//
//class FuncVarLiteralReference(
//    element: FuncVarLiteralMixin
//) : FuncReferenceImpl<FuncVarLiteralMixin>(element) {
//    override fun multiResolve(): Sequence<PsiElement> =
//        FuncResolver.resolveVarLiteralReference(element)
//}
