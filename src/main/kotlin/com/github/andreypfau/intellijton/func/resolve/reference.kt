package com.github.andreypfau.intellijton.func.resolve

import com.github.andreypfau.intellijton.func.psi.FuncElement
import com.github.andreypfau.intellijton.func.psi.FuncFunctionCallMixin
import com.github.andreypfau.intellijton.func.psi.FuncFunctionDefinitionMixin
import com.github.andreypfau.intellijton.func.psi.FuncSourceUnit
import com.github.andreypfau.intellijton.parentOfType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.childrenOfType

interface FuncReference : PsiPolyVariantReference {
    override fun getElement(): FuncElement

    override fun resolve(): FuncElement?

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult>
}

abstract class FuncReferenceImpl<T : FuncElement>(element: T) : PsiReferenceBase<T>(element), PsiPolyVariantReference {
    override fun resolve(): FuncElement? = null

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> = emptyArray()
}

class FuncFunctionCallReference(
    element: FuncFunctionCallMixin
) : FuncReferenceImpl<FuncFunctionCallMixin>(element), FuncReference {
    override fun resolve(): FuncElement? = multiResolve(false).firstOrNull()?.element as? FuncElement

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return findFunctions(incompleteCode)?.map {
            PsiElementResolveResult(it)
        }?.toList()?.also {
            println("function call resolve: $it")
        }?.toTypedArray() ?: emptyArray()
    }

    override fun getVariants(): Array<Any> = findFunctions()?.map {
        LookupElementBuilder.createWithIcon(it)
    }?.toList()?.also { println("variant resolve: $it") }?.toTypedArray() ?: emptyArray()

    fun findFunctions(incompleteCode: Boolean = true): Sequence<FuncFunctionDefinitionMixin>? {
        println("Resolving: $element")
        return element.parentOfType<FuncSourceUnit>()
            ?.childrenOfType<FuncFunctionDefinitionMixin>()
            ?.asSequence()
            ?.filter {
                val elementName = element.name ?: return@filter false
                if (incompleteCode) {
                    it.name?.contains(elementName) == true
                } else {
                    it.name == elementName
                }
            }
    }
}
