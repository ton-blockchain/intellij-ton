package org.ton.intellij.tact

import com.intellij.lang.InjectableLanguage
import com.intellij.lang.Language

object TactLanguage : Language(
    "tact", "tact", "text/tact", "text/x-tact", "application/x-tact"
), InjectableLanguage
