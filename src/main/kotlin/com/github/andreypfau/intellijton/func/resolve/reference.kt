package com.github.andreypfau.intellijton.func.resolve

import com.github.andreypfau.intellijton.func.psi.*
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.elementType

interface FuncReference : PsiReference {
    override fun getElement(): FuncElement
    override fun resolve(): FuncElement?
    fun multiResolve(): Sequence<PsiElement>
}

abstract class FuncReferenceBase<T : FuncNamedElement>(
    element: T
) : PsiPolyVariantReferenceBase<T>(element), FuncReference {
    override fun calculateDefaultRangeInElement() = TextRange(0, element.textRange.length)
    override fun getVariants(): Array<Any> = emptyArray()
    override fun resolve(): FuncElement? = super.resolve() as? FuncElement
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> =
        multiResolve().map(::PsiElementResolveResult).toList().toTypedArray()

    override fun handleElementRename(newElementName: String): PsiElement {
        val name = element.nameIdentifier ?: return element
        doRename(name, newElementName)
        return element
    }
    protected open fun doRename(identifier: PsiElement, newName: String) {
        check(identifier.elementType == FuncTypes.IDENTIFIER)
        identifier.replace(identifier.project.funcPsiFactory.createIdentifier(newName.replace(".fc", "")))
    }
}

class FuncFunctionCallReference(
    element: FuncFunctionCall,
) : FuncReferenceBase<FuncFunctionCall>(element), FuncReference {
    override fun calculateDefaultRangeInElement(): TextRange = element.functionCallIdentifier.textRange
    fun resolveFunctionCall(): Sequence<FuncFunctionDefinition> {
        val element = element
        val name = element.functionCallIdentifier.identifier.text
        val file = element.resolveFile()
        return file.resolveAllFunctions().filter {
            it.name == name
        }
    }

    override fun multiResolve(): Sequence<PsiElement> = resolveFunctionCall()
}

class FuncFunctionCallIdentifierReference(
    element: FuncFunctionCallIdentifier
) : FuncReferenceBase<FuncFunctionCallIdentifier>(element) {
    override fun multiResolve(): Sequence<PsiElement> =
        (element.parent as FuncFunctionCallMixin).reference.multiResolve()
}

