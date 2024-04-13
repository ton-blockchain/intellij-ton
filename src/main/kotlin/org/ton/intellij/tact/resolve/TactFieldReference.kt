package org.ton.intellij.tact.resolve

import com.intellij.openapi.util.TextRange
import org.ton.intellij.tact.psi.TactElement
import org.ton.intellij.tact.psi.TactInferenceContextOwner
import org.ton.intellij.tact.type.selfInferenceResult
import org.ton.intellij.util.ancestorStrict

class TactFieldReference<T : TactElement>(element: T, range: TextRange) : TactReferenceBase<T>(
    element, range
) {
    override fun multiResolve(): Collection<TactElement> {
        val inference = element.ancestorStrict<TactInferenceContextOwner>()?.selfInferenceResult
        return inference?.getResolvedRefs(element)?.mapNotNull { it.element as? TactElement } ?: emptyList()
    }
}
