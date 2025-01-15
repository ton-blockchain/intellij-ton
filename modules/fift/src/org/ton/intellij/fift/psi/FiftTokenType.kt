package org.ton.intellij.fift.psi

import com.intellij.psi.tree.IElementType
import org.ton.intellij.fift.FiftLanguage

class FiftTokenType(debugName: String) : IElementType(debugName, FiftLanguage) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FiftTokenType
        return index == other.index
    }

    override fun hashCode(): Int = index.toInt()
}
