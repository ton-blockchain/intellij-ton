package org.ton.intellij.func.template

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.ton.intellij.func.psi.FuncElementTypes

class FuncEverywhereContextType : FuncTemplateContextType("func") {
    override fun isInContext(o: PsiElement): Boolean {
        return !(o is PsiComment || o is LeafPsiElement && o.elementType == FuncElementTypes.RAW_STRING_ELEMENT)
    }
}
