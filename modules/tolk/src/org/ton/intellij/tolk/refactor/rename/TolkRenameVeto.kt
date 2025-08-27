package org.ton.intellij.tolk.refactor.rename

import com.intellij.openapi.util.Condition
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.psi.TolkSelfParameter

class TolkRenameVeto : Condition<PsiElement> {
    override fun value(element: PsiElement): Boolean {
        return element is TolkSelfParameter
    }
}
