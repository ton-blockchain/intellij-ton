package org.ton.intellij.tolk.template

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.ton.intellij.tolk.psi.TolkElementTypes

class TolkEverywhereContextType : TolkTemplateContextType("tolk") {
    override fun isInContext(o: PsiElement): Boolean {
        return !(o is PsiComment || o is LeafPsiElement && o.elementType == TolkElementTypes.RAW_STRING_ELEMENT)
    }
}
