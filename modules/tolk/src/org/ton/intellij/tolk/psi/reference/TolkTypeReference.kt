package org.ton.intellij.tolk.psi.reference

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.parentOfType
import org.ton.intellij.tolk.psi.*

class TolkTypeReference(
    element: TolkReferenceElement,
) : PsiReferenceBase.Poly<TolkReferenceElement>(
    element
) {
    override fun calculateDefaultRangeInElement(): TextRange {
        val identifier = element.referenceNameElement ?: return TextRange.EMPTY_RANGE
        return TextRange(identifier.startOffsetInParent, identifier.textLength)
    }

    override fun resolve(): TolkTypedElement? {
        return super.resolve() as? TolkTypedElement?
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val myElement = myElement
        return buildList<ResolveResult> {
            val typeParameterName = element.referenceName ?: return@buildList
            val owner = myElement.parentOfType<TolkTypeParameterListOwner>()
            if (owner != null) {
                if (typeParameterName == "self" && owner is TolkFunction) {
                    val selfParameter = owner.parameterList?.parameterList?.find { it.name == "self" }
                    if (selfParameter != null) {
                        add(PsiElementResolveResult(selfParameter))
                    }
                }

                if ((myElement !is TolkReferenceTypeExpression || myElement.typeArgumentList == null) && myElement.parentOfType<TolkFunctionReceiver>() == null) {
                    val genericType = owner.resolveGenericType(typeParameterName)
                    if (genericType != null && genericType.parameter.psi != element) {
                        add(PsiElementResolveResult(genericType.parameter.psi))
                        // already resolved as type parameter, no need to resolve it further
                        return@buildList
                    }
                }
            }

            val file = myElement.containingFile as? TolkFile ?: return@buildList
            val typeDefResults = collectTypeDefResults(file, typeParameterName)
            addAll(typeDefResults)
        }.toTypedArray()
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val identifier = element.referenceNameElement ?: return super.handleElementRename(newElementName)
        val newId = TolkPsiFactory[identifier.project].createIdentifier(newElementName)
        identifier.replace(newId)
        return element
    }

    private fun collectTypeDefResults(file: TolkFile, target: String): List<PsiElementResolveResult> {
        val injectedLanguageManager = InjectedLanguageManager.getInstance(file.project)
        val isInjected = injectedLanguageManager.isInjectedFragment(file)
        if (isInjected) {
            val hostElement = injectedLanguageManager.getInjectionHost(file)
            val originalFile = hostElement?.containingFile as? TolkFile
            if (originalFile != null) {
                return collectTypeDefResults(originalFile, target)
            }
        }

        return file.resolveSymbols(target)
            .filterIsInstance<TolkTypeSymbolElement>()
            .map { PsiElementResolveResult(it) }
            .toList()
    }
}
