package org.ton.intellij.func.psi.impl

import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet
import org.ton.intellij.func.psi.FuncIncludeDefinition

class FuncIncludePathReference(
    element: FuncIncludeDefinition,
) : FileReferenceSet(
    element.path,
    element,
    element.textOffset,
    null,
    true
) {
}
