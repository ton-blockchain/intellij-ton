package org.ton.intellij.tlb

import com.intellij.lang.InjectableLanguage
import com.intellij.lang.Language

object TlbLanguage :
    Language("tlb", "text/tlb", "text/x-tlb", "text/tl-b", "text/x-tl-b", "application/x-tlb", "application/x-tl-b"),
    InjectableLanguage {
    override fun isCaseSensitive(): Boolean = false
}
