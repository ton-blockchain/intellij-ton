package org.ton.intellij.tact.resolve

import com.intellij.openapi.util.TextRange
import org.ton.intellij.tact.psi.TactElement
import org.ton.intellij.tact.psi.TactTypeDeclarationElement
import org.ton.intellij.tact.stub.index.TactTypesIndex

class TactTypeReference<T : TactElement>(element: T, range: TextRange) : TactReferenceBase<T>(
    element, range
) {
    override fun multiResolve(): Collection<TactTypeDeclarationElement> {
        val currentFile = element.containingFile
        val result = TactTypesIndex.findElementsByName(element.project, value)
        val localType = result.asSequence()
            .filterIsInstance<TactTypeDeclarationElement>()
            .find { it.containingFile == currentFile }
        if (localType != null) {
            return listOf(localType)
        }
        return listOf(result.firstOrNull() as? TactTypeDeclarationElement ?: return emptyList())
    }
}
