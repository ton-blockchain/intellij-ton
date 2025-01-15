package org.ton.intellij.tact.resolve

import com.intellij.openapi.util.TextRange
import org.ton.intellij.tact.psi.TactElement
import org.ton.intellij.tact.psi.TactTypeDeclarationElement
import org.ton.intellij.tact.type.TactTy

class TactTypeReference<T : TactElement>(element: T, range: TextRange) : TactReferenceBase<T>(
    element, range
) {
    override fun multiResolve(): Collection<TactTypeDeclarationElement> {
        return listOf(TactTy.searchDeclarations(element, value).firstOrNull() ?: return emptyList())
    }
}
