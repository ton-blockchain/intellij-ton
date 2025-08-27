package org.ton.intellij.tolk.refactor.rename

import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import org.ton.intellij.tolk.psi.TolkNamedElement
import org.ton.intellij.tolk.psi.TolkSelfParameter

class TolkRenameProcessor : RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean {
        if (element is TolkSelfParameter) {
            return false
        }
        return element is TolkNamedElement
    }
}
