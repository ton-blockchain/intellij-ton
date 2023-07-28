package org.ton.intellij.func.template

import com.intellij.codeInsight.template.TemplateActionContext
import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilCore
import org.ton.intellij.func.FuncLanguage

abstract class FuncTemplateContextType(name: String) : TemplateContextType(name) {
    override fun isInContext(ctx: TemplateActionContext): Boolean {
        if (!PsiUtilCore.getLanguageAtOffset(ctx.file, ctx.startOffset).isKindOf(FuncLanguage)) return false
        val psiElement = ctx.file.findElementAt(ctx.startOffset)
        return psiElement != null && isInContext(psiElement)
    }

    abstract fun isInContext(o: PsiElement): Boolean
}
