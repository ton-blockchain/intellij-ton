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
        val newIdentifier = identifier.project.tlbPsiFactory.createFromText<TlbConstructorName>("$newName#_ = DUMMY")
        identifier.replace(requireNotNull(newIdentifier?.identifier))
    }
}

class TlbNamedRefReference(
    element: TlbNamedRef
) : TlbReferenceBase<TlbNamedRef>(element) {
    override fun multiResolve(): Sequence<TlbElement> {
        if (element.parent !is TlbTypeExpression) return emptySequence()

        val anonymousConstructor = element.parentOfType<TlbAnonymousConstructor>()
        val currentCombinatorDeclaration =
            element.parentOfType<TlbCombinatorDeclaration>() ?: return resolveCombinators()

        val fields =
            resolveFields(anonymousConstructor) + resolveFields(currentCombinatorDeclaration)
        if (fields.isNotEmpty()) return fields.asSequence()

        val implicitFields =
            resolveImplicitFields(anonymousConstructor) + resolveImplicitFields(currentCombinatorDeclaration)
        if (implicitFields.isNotEmpty()) return implicitFields.asSequence()

        return resolveCombinators()
    }

    private fun resolveFields(combinatorDeclaration: TlbCombinatorDeclaration) =
        combinatorDeclaration.resolveFields().filter { namedField ->
            namedField.fieldName.textMatches(element)
        }.toList()

    private fun resolveFields(anonymousConstructor: TlbAnonymousConstructor?) =
        anonymousConstructor.resolveFields().filter { namedField ->
            namedField.fieldName.textMatches(element)
        }.toList()

    private fun resolveImplicitFields(combinatorDeclaration: TlbCombinatorDeclaration) =
        combinatorDeclaration.resolveImplicitFields().filter { implicitField ->
            implicitField.implicitFieldName.textMatches(element)
        }.toList()

    private fun resolveImplicitFields(anonymousConstructor: TlbAnonymousConstructor?) =
        anonymousConstructor.resolveImplicitFields().filter { implicitField ->
            implicitField.implicitFieldName.textMatches(element)
        }.toList()

    private fun resolveCombinators() = element.resolveFile()
        .resolveAllCombinatorDeclarations()
        .map {
            it.combinator?.combinatorName
        }
        .filterNotNull()
        .filter {
            it.textMatches(element.identifier)
        }
}

