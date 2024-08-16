package org.ton.intellij.tolk.ide

import com.intellij.lang.injection.general.Injection
import com.intellij.lang.injection.general.LanguageInjectionContributor
import com.intellij.lang.injection.general.SimpleInjection
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import org.ton.intellij.fift.FiftLanguage
import org.ton.intellij.tolk.psi.TolkAsmBody
import org.ton.intellij.tolk.psi.TolkElementTypes

class TolkLanguageInjector : LanguageInjectionContributor {
    override fun getInjection(p0: PsiElement): Injection? {
        if (p0.elementType != TolkElementTypes.RAW_STRING) return null
        PsiTreeUtil.findFirstParent(p0) { it is TolkAsmBody } ?: return null

        return SimpleInjection(
            FiftLanguage, "", "", null
        )
    }
}
