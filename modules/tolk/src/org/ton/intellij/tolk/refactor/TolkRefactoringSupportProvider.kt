package org.ton.intellij.tolk.refactor

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.psi.TolkNamedElement
import org.ton.intellij.tolk.psi.TolkSelfParameter

class TolkRefactoringSupportProvider : RefactoringSupportProvider() {
    override fun isMemberInplaceRenameAvailable(element: PsiElement, context: PsiElement?): Boolean {
        if (element is TolkSelfParameter) {
            return false
        }
        return element is TolkNamedElement
    }

    override fun getIntroduceVariableHandler() = TolkIntroduceVariableHandler()
}
