package org.ton.intellij.tolk.acton

import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import org.ton.intellij.acton.cli.ActonToml
import org.ton.intellij.tolk.psi.TolkContractDefinition

class TolkContractDefinitionReference(element: TolkContractDefinition) :
    PsiReferenceBase<TolkContractDefinition>(element, element.contractNameRange(), false),
    HighlightedReference {
    override fun resolve(): PsiElement? {
        val contractName = element.name ?: return null
        val sourceVirtualFile = element.containingFile.originalFile.virtualFile ?: return null
        val actonToml = ActonToml.find(element.project, sourceVirtualFile) ?: return null
        return actonToml.getContractElements().find { it.name == contractName }
    }

    override fun getVariants(): Array<Any> = emptyArray()
}

private fun TolkContractDefinition.contractNameRange(): TextRange {
    val identifier = identifier ?: return TextRange.EMPTY_RANGE
    return TextRange(identifier.startOffsetInParent, identifier.startOffsetInParent + identifier.textLength)
}
