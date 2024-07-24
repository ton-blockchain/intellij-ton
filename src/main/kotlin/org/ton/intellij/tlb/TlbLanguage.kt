package org.ton.intellij.tlb

import com.intellij.lang.InjectableLanguage
import com.intellij.lang.Language

object TlbLanguage : Language(
    "TypeLanguage-Binary",
    "tl-b",
    "tlb",
    "text/tlb",
    "text/x-tlb",
    "text/tl-b",
    "text/x-tl-b",
    "application/x-tlb",
    "application/x-tl-b"
), InjectableLanguage {
    private fun readResolve(): Any = TlbLanguage

    override fun isCaseSensitive(): Boolean = false
}
