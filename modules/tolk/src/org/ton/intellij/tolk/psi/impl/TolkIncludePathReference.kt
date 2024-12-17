package org.ton.intellij.tolk.psi.impl

import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet
import org.ton.intellij.tolk.psi.TolkIncludeDefinition
import org.ton.intellij.tolk.sdk.TolkSdkManager

class TolkIncludePathReference(
    element: TolkIncludeDefinition,
) : FileReferenceSet(
    if (element.path.startsWith("@stdlib/")) {
        TolkSdkManager[element.project].getSdkRef().resolve(element.project)?.stdlibFile?.path + "/" +
                element.path.substring("@stdlib/".length)
    } else {
        element.path
    },
    element,
    if (element.path.startsWith("@stdlib/")) {
        "@stdlib/".length
    } else {
        0
    },
    null,
    true
)
