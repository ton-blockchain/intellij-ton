package com.github.andreypfau.intellijton.func.psi

import com.github.andreypfau.intellijton.func.FuncLanguage
import com.intellij.psi.tree.IElementType

class FuncTokenType(debugName: String) : IElementType(debugName, FuncLanguage) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FuncTokenType
        if (index != other.index) return false
        return true
    }

    override fun hashCode(): Int = index.toInt()
}

