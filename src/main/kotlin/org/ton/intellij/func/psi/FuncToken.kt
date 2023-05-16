package org.ton.intellij.func.psi

import com.intellij.psi.tree.IElementType
import org.ton.intellij.func.FuncLanguage

class FuncToken(
    debug: String
) : IElementType(
    debug, FuncLanguage
)
