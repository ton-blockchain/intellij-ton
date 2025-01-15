package org.ton.intellij.tact.resolve

import com.intellij.openapi.util.TextRange
import org.ton.intellij.tact.psi.TactElement
import org.ton.intellij.tact.psi.TactInferenceContextOwner
import org.ton.intellij.tact.type.selfInferenceResult
import org.ton.intellij.util.ancestorStrict

open class TactFieldReference<T : TactElement>(element: T, range: TextRange) : TactReferenceBase<T>(
    element, range
) {
    override fun multiResolve(): Collection<TactElement> {
        val inference = element.ancestorStrict<TactInferenceContextOwner>()?.selfInferenceResult
        if (inference != null) {
            val resolvedRefs = inference.getResolvedRefs(element).mapNotNull { it.element as? TactElement }
            val currentFIle = element.containingFile
            val currentFileCandidates = resolvedRefs.filter { it.containingFile == currentFIle }
            return currentFileCandidates.ifEmpty {
                resolvedRefs
            }
        }
        return emptyList()
    }
}
