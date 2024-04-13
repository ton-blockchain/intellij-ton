package org.ton.intellij.tact.resolve

import com.intellij.openapi.util.TextRange
import org.ton.intellij.tact.psi.TactElement
import org.ton.intellij.tact.stub.index.TactTypesIndex

class TactTypeReference<T : TactElement>(element: T, range: TextRange) : TactReferenceBase<T>(
    element, range
) {
    override fun multiResolve(): Collection<TactElement> {
        val result = TactTypesIndex.findElementsByName(element.project, value)
        return result
    }
}
