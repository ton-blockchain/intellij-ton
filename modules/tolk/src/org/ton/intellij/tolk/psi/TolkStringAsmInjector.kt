package org.ton.intellij.tolk.psi

import com.intellij.psi.InjectedLanguagePlaces
import com.intellij.psi.LanguageInjector
import com.intellij.psi.PsiLanguageInjectionHost
import org.ton.intellij.asm.AsmLanguage

class TolkStringAsmInjector : LanguageInjector {
    override fun getLanguagesToInject(
        host: PsiLanguageInjectionHost,
        places: InjectedLanguagePlaces
    ) {
        if (host !is TolkStringLiteral) return
        if (!host.isValidHost()) return
        val text = host.rawString ?: return
        places.addPlace(AsmLanguage, text.textRangeInParent, null, null)
    }
}
