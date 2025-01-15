package org.ton.intellij.asm

import com.intellij.lang.InjectableLanguage
import com.intellij.lang.Language

object AsmLanguage : Language("TVM assembly"), InjectableLanguage {
    override fun isCaseSensitive(): Boolean = true

    override fun getDisplayName(): String = "TVM assembly"
}
