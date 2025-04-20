package org.ton.intellij.tolk.psi

import org.ton.intellij.tolk.type.TolkType

interface TolkSymbolElement : TolkNamedElement, TolkTypedElement {
}

interface TolkTypeSymbolElement : TolkSymbolElement {
    override val type: TolkType
}
