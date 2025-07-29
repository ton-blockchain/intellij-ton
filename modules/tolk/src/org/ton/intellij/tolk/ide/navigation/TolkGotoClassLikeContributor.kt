package org.ton.intellij.tolk.ide.navigation

import org.ton.intellij.tolk.psi.TolkStruct
import org.ton.intellij.tolk.stub.index.TolkStructIndex

open class TolkGotoClassLikeContributor :
    TolkGotoContributorBase<TolkStruct>(TolkStruct::class.java, TolkStructIndex.KEY)
