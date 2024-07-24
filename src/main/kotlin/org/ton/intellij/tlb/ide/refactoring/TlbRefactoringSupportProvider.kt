package org.ton.intellij.tlb.ide.refactoring

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.PsiElement
import org.ton.intellij.tlb.psi.TlbNamedElement

class TlbRefactoringSupportProvider : RefactoringSupportProvider() {
    override fun isMemberInplaceRenameAvailable(element: PsiElement, context: PsiElement?): Boolean {
        return element is TlbNamedElement
    }
}
