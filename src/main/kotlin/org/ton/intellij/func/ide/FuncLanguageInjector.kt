package org.ton.intellij.func.ide

import com.intellij.lang.injection.general.Injection
import com.intellij.lang.injection.general.LanguageInjectionContributor
import com.intellij.lang.injection.general.SimpleInjection
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import org.ton.intellij.fift.FiftLanguage
import org.ton.intellij.func.psi.FuncAsmBody
import org.ton.intellij.func.psi.FuncElementTypes

class FuncLanguageInjector : LanguageInjectionContributor {
    override fun getInjection(p0: PsiElement): Injection? {
        if (p0.elementType != FuncElementTypes.RAW_STRING) return null
        println("try: '${p0.text}'")
        PsiTreeUtil.findFirstParent(p0) { it is FuncAsmBody } ?: return null
        println("find parret!")

        return SimpleInjection(
            FiftLanguage, "", "", null
        ).also {
            println("injection: '${p0.text}'")
        }
    }
}
