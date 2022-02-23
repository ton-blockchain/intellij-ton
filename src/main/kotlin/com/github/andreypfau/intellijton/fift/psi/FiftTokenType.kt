package com.github.andreypfau.intellijton.fift.psi

import com.github.andreypfau.intellijton.fift.FiftLanguage
import com.intellij.psi.tree.IElementType

class FiftTokenType(debugName: String) : IElementType(debugName, FiftLanguage)  {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FiftTokenType
        if (index != other.index) return false
        return true
    }

    override fun hashCode(): Int = index.toInt()
}
