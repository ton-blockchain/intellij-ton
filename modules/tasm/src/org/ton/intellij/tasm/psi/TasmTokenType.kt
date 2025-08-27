package org.ton.intellij.tasm.psi

import com.intellij.psi.tree.IElementType
import org.ton.intellij.tasm.TasmLanguage

class TasmTokenType(debugName: String) : IElementType(debugName, TasmLanguage) {
    override fun toString(): String = "TasmTokenType." + super.toString()
}
