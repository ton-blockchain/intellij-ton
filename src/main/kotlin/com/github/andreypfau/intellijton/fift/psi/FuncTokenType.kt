package com.github.andreypfau.intellijton.fift.psi

import com.github.andreypfau.intellijton.func.FuncLanguage
import com.intellij.psi.tree.IElementType

class FiftTokenType(debugName: String) : IElementType(debugName, FiftLanguage) {
    override fun toString(): String = "FuncTokenType.${super.toString()}"
}

