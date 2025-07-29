package org.ton.intellij.tolk.ide.navigation

import org.ton.intellij.tolk.psi.TolkNamedElement
import org.ton.intellij.tolk.stub.index.TolkNamedElementIndex

open class TolkGotoSymbolContributor :
    TolkGotoContributorBase<TolkNamedElement>(TolkNamedElement::class.java, TolkNamedElementIndex.KEY)
