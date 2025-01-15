package org.ton.intellij.tolk

import com.intellij.lang.InjectableLanguage
import com.intellij.lang.Language

object TolkLanguage : Language(
    "Tolk", "tolk", "text/tolk", "text/x-tolk", "application/x-tolk"
), InjectableLanguage {
    override fun isCaseSensitive(): Boolean = false

    override fun getDisplayName(): String = "Tolk"
}
