package org.ton.intellij.func

import com.intellij.lang.InjectableLanguage
import com.intellij.lang.Language

object FuncLanguage : Language(
    "FunC", "func", "text/func", "text/x-func", "application/x-func"
), InjectableLanguage {
    override fun isCaseSensitive(): Boolean = false

    override fun getDisplayName(): String = "FunC"
}
