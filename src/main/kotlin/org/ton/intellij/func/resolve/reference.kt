package org.ton.intellij.func.resolve

import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.util.elementType
import org.ton.intellij.func.psi.*

interface FuncReference : PsiReference {
    override fun getElement(): FuncElement
    override fun resolve(): FuncElement?
    fun multiResolve(): Sequence<FuncElement>
}

abstract class FuncReferenceBase<T : FuncNamedElement>(
    element: T
) : PsiPolyVariantReferenceBase<T>(element), FuncReference {
    val file = element.resolveFile()

    override fun calculateDefaultRangeInElement() = TextRange(0, element.textRange.length)
    override fun getVariants(): Array<Any> = emptyArray()
    override fun resolve(): FuncElement? = super.resolve() as? FuncElement
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val multiFileResolve = multiResolve().map(::PsiElementResolveResult).toList()
        val currentFileResolve = multiFileResolve.filter { it.element.containingFile == file }
        return currentFileResolve.ifEmpty { multiFileResolve }.toTypedArray()
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val name = element.nameIdentifier ?: return element
        doRename(name, newElementName)
        return element
    }

    protected open fun doRename(identifier: PsiElement, newName: String) {
        check(identifier.elementType == FuncTokenTypes.IDENTIFIER)
        identifier.replace(identifier.project.funcPsiFactory.createIdentifier(newName.replace(".fc", "")))
    }
}

class FuncFunctionCallReference(
    element: FuncFunctionCall,
) : FuncReferenceBase<FuncFunctionCall>(element), FuncReference {
    override fun calculateDefaultRangeInElement(): TextRange = element.functionCallIdentifier.textRange
    override fun multiResolve(): Sequence<FuncFunction> {
        val name = element.functionCallIdentifier.identifier.text
        return file.resolveAllFunctions().filter { funcFunction ->
            funcFunction.name == name
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
    override fun calculateDefaultRangeInElement(): TextRange =
        element.methodCallIdentifier?.textRange ?: super.calculateDefaultRangeInElement()

    override fun multiResolve(): Sequence<FuncFunction> {
        val params = element.tensorExpression?.tensorExpressionItemList ?: emptyList()
        val name = element.name ?: return emptySequence()
        return file.resolveAllFunctions().filter { funcFunction ->
            val paramList = funcFunction.parameterList?.parameterDeclarationList ?: return@filter false
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
    val params = element.tensorExpression.tensorExpressionItemList

    override fun calculateDefaultRangeInElement(): TextRange =
        element.modifyingMethodCallIdentifier.textRange

    override fun multiResolve(): Sequence<FuncFunction> {
        return file.resolveAllFunctions().filter { funcFunction ->
            val paramList = funcFunction.parameterList?.parameterDeclarationList ?: return@filter false
            val functionName = funcFunction.functionName ?: return@filter false
            (paramList.size - 1) == params.size && functionName.identifier.textMatches(element.modifyingMethodCallIdentifier.identifier)
        }
    }
}

class FuncModifyingMethodCallIdentifierReference(
    element: FuncModifyingMethodCallIdentifier
) : FuncReferenceBase<FuncModifyingMethodCallIdentifier>(element) {
    override fun multiResolve() = (element.parent as FuncModifyingMethodCallMixin).reference.multiResolve()
}

class FuncReferenceExpressionReference(
    element: FuncReferenceExpression
) : FuncReferenceBase<FuncReferenceExpression>(element), FuncReference {
    val elementName = element.identifier.text
    val elementTextOffset = element.textOffset
    val funcFile = element.resolveFile()

    override fun multiResolve(): Sequence<FuncElement> {
        return funcFile.resolveReferenceExpressionProviders(elementTextOffset).filter {
            it != element && it.identifyingElement?.textMatches(elementName) == true
        }
    }
}

class FuncIncludePathReference(element: FuncIncludePath) : FuncReferenceBase<FuncIncludePath>(element) {
    override fun multiResolve(): Sequence<FuncElement> {
        if (element.textLength < 3) return emptySequence()
        var includeText = element.text
        includeText = includeText.substring(1, includeText.lastIndex)
        val virtualFile = element.containingFile.originalFile.virtualFile
        val file = findIncludeFile(virtualFile, includeText)
        if (file == null) {
            return emptySequence()
        }
        val funcFile = PsiManager.getInstance(element.project).findFile(file) as? FuncFile ?: return emptySequence()
        return sequenceOf(funcFile)
    }

    fun findIncludeFile(file: VirtualFile, path: String) = file.findFileByRelativePath("../$path")

    override fun getVariants(): Array<Any> {
        return super.getVariants()
    }
}