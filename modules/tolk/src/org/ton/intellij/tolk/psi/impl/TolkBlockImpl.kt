package org.ton.intellij.tolk.psi.impl

import com.intellij.psi.util.elementType
import org.ton.intellij.tolk.psi.TolkBlockStatement
import org.ton.intellij.tolk.psi.TolkElementTypes

val TolkBlockStatement.rBrace
    get() = lastChild?.let {
        if (it.elementType == TolkElementTypes.RBRACE) it else null
    }

val TolkBlockStatement.lBrace
    get() = firstChild?.let {
        if (it.elementType == TolkElementTypes.LBRACE) it else null
    }
