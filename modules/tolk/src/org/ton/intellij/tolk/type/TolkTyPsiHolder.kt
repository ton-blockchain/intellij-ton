package org.ton.intellij.tolk.type

import org.ton.intellij.tolk.psi.TolkElement

interface TolkTyPsiHolder : TolkTy {
    val psi: TolkElement
}
