package com.github.andreypfau.intellijton.tlb.resolve

import com.github.andreypfau.intellijton.tlb.psi.*
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType

interface TlbReference : PsiReference {
    override fun getElement(): TlbElement
    override fun resolve(): TlbElement?
    fun multiResolve(): Sequence<TlbElement>
}


abstract class TlbReferenceBase<T : TlbNamedElement>(
    element: T
) : PsiPolyVariantReferenceBase<T>(element), TlbReference {
    override fun calculateDefaultRangeInElement() = TextRange(0, element.textRange.length)
    override fun getVariants(): Array<Any> = emptyArray()
    override fun resolve(): TlbElement? = multiResolve().firstOrNull()
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> =
        multiResolve().map(::PsiElementResolveResult).toList().toTypedArray()

    override fun handleElementRename(newElementName: String): PsiElement {
        val name = element.nameIdentifier ?: return element
        doRename(name, newElementName)
        return element
    }

    protected open fun doRename(identifier: PsiElement, newName: String) {
        check(identifier.elementType == TlbTypes.IDENTIFIER)
        TODO()
//        identifier.replace(identifier.project.TlbPsiFactory.createIdentifier(newName.replace(".fc", "")))
    }
}

class TlbNamedRefReference(
    element: TlbNamedRef
) : TlbReferenceBase<TlbNamedRef>(element) {
    override fun multiResolve(): Sequence<TlbElement> {
        if (element.parent !is TlbTypeExpression) return emptySequence()
        val currentCombinatorDeclaration =
            element.parentOfType<TlbCombinatorDeclaration>() ?: return resolveCombinators()

        val fields = resolveFields(currentCombinatorDeclaration).toList()
        if (fields.isNotEmpty()) return fields.asSequence()

        val implicitFields = resolveImplicitFields(currentCombinatorDeclaration).toList()
        if (implicitFields.isNotEmpty()) return implicitFields.asSequence()

        return resolveCombinators()
    }

    private fun resolveFields(combinatorDeclaration: TlbCombinatorDeclaration) =
        combinatorDeclaration.resolveFields().map {
            it.fieldName
        }.filter { it.textMatches(element) }.toList()

    private fun resolveImplicitFields(combinatorDeclaration: TlbCombinatorDeclaration) =
        combinatorDeclaration.resolveImplicitFields().map {
            it.implicitFieldName
        }.filter {
            it.textMatches(element)
        }

    private fun resolveCombinators() = element.resolveFile()
        .resolveCombinatorDeclarations()
        .map {
            it.combinator?.combinatorName
        }
        .filterNotNull()
        .filter {
            it.textMatches(element.identifier)
        }
}

