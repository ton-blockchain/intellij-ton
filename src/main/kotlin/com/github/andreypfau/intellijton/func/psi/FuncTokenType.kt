package com.github.andreypfau.intellijton.func.psi

import com.github.andreypfau.intellijton.func.FuncLanguage
import com.intellij.psi.tree.IElementType

class FuncTokenType(debugName: String) : IElementType(debugName, FuncLanguage) {
    override fun toString(): String = "FuncTokenType.${super.toString()}"
}

