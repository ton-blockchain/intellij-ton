package com.github.andreypfau.intellijton.tlb.psi

import com.github.andreypfau.intellijton.tlb.TlbLanguage
import com.intellij.psi.tree.IElementType

@Suppress("EqualsOrHashCode")
class TlbTokenType(debugName: String) : IElementType(debugName, TlbLanguage) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }
}

