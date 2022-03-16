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
    val file = element.resolveFile()
    override fun calculateDefaultRangeInElement(): TextRange = element.functionCallIdentifier.textRange
    override fun multiResolve(): Sequence<FuncFunction> {
        val params = element.tensorExpression?.tensorExpressionItemList ?: emptyList()
        val name = element.functionCallIdentifier.identifier.text
        return file.resolveAllFunctions().filter { funcFunction ->
            val paramList = funcFunction.parameterList.parameterDeclarationList
            paramList.size == params.size && funcFunction.name == name
        }.sortedBy {
            if (it.resolveFile() == file) 1 else -1
        }
    }
}

class FuncFunctionCallIdentifierReference(
    element: FuncFunctionCallIdentifier
) : FuncReferenceBase<FuncFunctionCallIdentifier>(element) {
    override fun multiResolve() = (element.parent as FuncFunctionCallMixin).reference.multiResolve()
}

class FuncMethodCallReference(
    element: FuncMethodCall
) : FuncReferenceBase<FuncMethodCall>(element), FuncReference {
    val file = element.resolveFile()
    override fun calculateDefaultRangeInElement(): TextRange =
        element.methodCallIdentifier?.textRange ?: super.calculateDefaultRangeInElement()

    override fun multiResolve(): Sequence<FuncFunction> {
        val params = element.tensorExpression?.tensorExpressionItemList ?: emptyList()
        val name = element.name ?: return emptySequence()
        return file.resolveAllFunctions().filter { funcFunction ->
            val paramList = funcFunction.parameterList.parameterDeclarationList
            (paramList.size - 1) == params.size && funcFunction.name == name
        }
    }
}

class FuncMethodCallIdentifierReference(
    element: FuncMethodCallIdentifier
) : FuncReferenceBase<FuncMethodCallIdentifier>(element) {
    override fun multiResolve() = (element.parent as FuncMethodCallMixin).reference.multiResolve()
}

class FuncModifyingMethodCallReference(
    element: FuncModifyingMethodCall
) : FuncReferenceBase<FuncModifyingMethodCall>(element), FuncReference {
    val file = element.resolveFile()
    override fun calculateDefaultRangeInElement(): TextRange =
        element.modifyingMethodCallIdentifier?.textRange ?: super.calculateDefaultRangeInElement()

    // TODO
    override fun multiResolve(): Sequence<FuncFunction> {
        return emptySequence()
//        val params = element.tensorExpression?.tensorExpressionItemList ?: emptyList()
//        val name = element.name?.let { "~$it" } ?: return emptySequence()
//        return file.resolveAllFunctions().filter { funcFunction ->
//            val paramList = funcFunction.parameterList.parameterDeclarationList
//            (paramList.size - 1) == params.size && funcFunction.name == name
//        }
    }
}

class FuncModifyingMethodCallIdentifierReference(
    element: FuncModifyingMethodCallIdentifier
) : FuncReferenceBase<FuncModifyingMethodCallIdentifier>(element) {
    override fun multiResolve() = (element.parent as FuncModifyingMethodCallMixin).reference.multiResolve()
}