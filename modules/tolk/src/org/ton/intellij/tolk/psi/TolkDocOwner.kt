package org.ton.intellij.tolk.psi

import org.ton.intellij.tolk.doc.psi.TolkDocComment

interface TolkDocOwner : TolkElement {
    val doc: TolkDocComment?
}
