package org.ton.intellij

import com.intellij.lang.InjectableLanguage
import com.intellij.lang.Language

object FuncLanguage : Language(
    "func", "func", "text/func", "text/x-func", "application/x-func"
), InjectableLanguage
