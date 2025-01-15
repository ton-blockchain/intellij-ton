package org.ton.intellij.func.psi.impl

import com.intellij.psi.util.elementType
import org.ton.intellij.func.psi.FuncBlockStatement
import org.ton.intellij.func.psi.FuncElementTypes

val FuncBlockStatement.rBrace
    get() = lastChild?.let {
        if (it.elementType == FuncElementTypes.RBRACE) it else null
    }

val FuncBlockStatement.lBrace
    get() = firstChild?.let {
        if (it.elementType == FuncElementTypes.LBRACE) it else null
    }
