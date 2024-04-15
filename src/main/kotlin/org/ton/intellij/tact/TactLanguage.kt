package org.ton.intellij.tact

import com.intellij.lang.InjectableLanguage
import com.intellij.lang.Language

object TactLanguage : Language(
    "Tact", "tact", "text/tact", "text/x-tact", "application/x-tact"
), InjectableLanguage {
    override fun isCaseSensitive(): Boolean {
        return false
    }

    override fun getDisplayName(): String {
        return "Tact"
    }
}
