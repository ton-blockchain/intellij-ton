package org.ton.intellij.tasm

import com.intellij.lang.InjectableLanguage
import com.intellij.lang.Language

object TasmLanguage : Language("TASM", "tasm", "text/tasm", "text/x-tasm", "application/x-tasm"), InjectableLanguage {
    private fun readResolve(): Any = TasmLanguage
    override fun getDisplayName() = "TASM"
}
