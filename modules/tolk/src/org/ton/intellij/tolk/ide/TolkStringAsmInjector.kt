package org.ton.intellij.tolk.ide

import com.intellij.psi.InjectedLanguagePlaces
import com.intellij.psi.LanguageInjector
import com.intellij.psi.PsiLanguageInjectionHost
import org.ton.intellij.asm.AsmLanguage
import org.ton.intellij.tolk.psi.TolkAsmDefinition
import org.ton.intellij.tolk.psi.TolkStringLiteral
import org.ton.intellij.util.parentOfType

class TolkStringAsmInjector : LanguageInjector {
    override fun getLanguagesToInject(
        host: PsiLanguageInjectionHost,
        places: InjectedLanguagePlaces,
    ) {
        if (host !is TolkStringLiteral) return
        if (!host.isValidHost) return
        if (host.parentOfType<TolkAsmDefinition>() == null) return
        val text = host.rawString ?: return
        places.addPlace(AsmLanguage, text.textRangeInParent, null, null)
    }
}
