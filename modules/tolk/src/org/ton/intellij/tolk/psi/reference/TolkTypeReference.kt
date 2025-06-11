package org.ton.intellij.tolk.psi.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.parentOfType
import org.ton.intellij.tolk.ide.configurable.tolkSettings
import org.ton.intellij.tolk.psi.*
import java.util.*

class TolkTypeReference(
    element: TolkElement,
) : PsiReferenceBase.Poly<TolkElement>(
    element
) {
    val identifier get() = element.node.findChildByType(TolkElementTypes.IDENTIFIER)!!

    override fun calculateDefaultRangeInElement(): TextRange {
        val identifier = identifier
        return TextRange(identifier.startOffsetInParent, identifier.textLength)
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val myElement = myElement
        return buildList<ResolveResult> {
            val typeParameterName = identifier.text.removeSurrounding("`")
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
                    }
                }
            }

            val file = myElement.containingFile as? TolkFile ?: return@buildList
            val typeDefResults = collectTypeDefResults(file, typeParameterName)
            addAll(typeDefResults)
        }.toTypedArray()
    }

    override fun handleElementRename(newElementName: String): PsiElement? {
        return identifier.psi.replace(TolkPsiFactory[element.project].createIdentifier(newElementName))
    }

    private fun collectTypeDefResults(file: TolkFile, target: String): List<PsiElementResolveResult> {
        val result = LinkedList<PsiElementResolveResult>()
        val visitedFiles = HashSet<TolkFile>()
        if (visitedFiles.add(file)) {
            file.declaredSymbols[target]?.forEach {
                result.add(PsiElementResolveResult(it))
            }
        }

        file.project.tolkSettings.getDefaultImport()?.let {
            if (visitedFiles.add(it)) {
                it.declaredSymbols[target]?.forEach {
                    result.add(PsiElementResolveResult(it))
                }
            }
        }

        file.getImportedFiles().forEach { importedFile ->
            if (visitedFiles.add(importedFile)) {
                importedFile.declaredSymbols[target]?.forEach {
                    result.add(PsiElementResolveResult(it))
                }
            }
        }

        return result
    }
}
