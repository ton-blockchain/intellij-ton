package org.ton.intellij.func.ide.navigation

import org.ton.intellij.func.psi.FuncNamedElement
import org.ton.intellij.func.stub.index.FuncNamedElementIndex

open class FuncGotoSymbolContributor :
    FuncGotoContributorBase<FuncNamedElement>(FuncNamedElement::class.java, FuncNamedElementIndex.KEY)
