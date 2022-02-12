package com.github.andreypfau.intellijton.func.resolve

import com.github.andreypfau.intellijton.func.psi.FuncElement
import com.github.andreypfau.intellijton.func.psi.FuncFunctionCall
import com.github.andreypfau.intellijton.func.psi.FuncFunctionDefinition
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

class FuncFunctionCallReference(
    element: FuncFunctionCall
) : FuncReferenceBase<FuncFunctionCall>(element), FuncReference {
    override fun calculateDefaultRangeInElement(): TextRange {
        val range = super.calculateDefaultRangeInElement()
        println("element=${element.text} range: $range")
        val ref = element.identifier.reference
        println("ref: $ref")
        return TextRange.create(0, range.length+5)
    }

    fun resolveFunctionCall(): Sequence<FuncFunctionDefinition> {
        val element = element
        val name = element.name ?: return emptySequence()
        val file = element.resolveFile()

        val parameterCount = element.tensorExpression?.children?.size ?: 0

        val stdlibFunctions = file.resolveStdlibFile()?.resolveFunctions() ?: emptySequence()
        val fileFunctions = file.resolveFunctions()
        val functions =  (stdlibFunctions + fileFunctions).filter {
            it.name == name
        }.filter {
            it.parameterList.parameterDeclarationList.size == parameterCount
        }.toList()
        return functions.asSequence()
    }

    override fun multiResolve(): Sequence<PsiElement> = resolveFunctionCall()
}

