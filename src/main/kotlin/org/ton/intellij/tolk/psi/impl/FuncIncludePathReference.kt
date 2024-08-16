package org.ton.intellij.tolk.psi.impl

import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet
import org.ton.intellij.tolk.psi.TolkIncludeDefinition

class TolkIncludePathReference(
    element: TolkIncludeDefinition,
) : FileReferenceSet(
    element.path,
    element,
    element.textOffset,
    null,
    true
) {
}
