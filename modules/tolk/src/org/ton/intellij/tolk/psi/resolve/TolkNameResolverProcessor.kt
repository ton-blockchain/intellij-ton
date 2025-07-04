package org.ton.intellij.tolk.psi.resolve

import com.intellij.psi.PsiElement
import com.intellij.psi.ResolveState
import com.intellij.psi.scope.DelegatingScopeProcessor
import com.intellij.psi.scope.PsiScopeProcessor
import org.ton.intellij.tolk.psi.TolkNamedElement

class TolkNameResolverProcessor(
    val name: String,
    delegate: PsiScopeProcessor
) : DelegatingScopeProcessor(delegate) {
    override fun execute(element: PsiElement, state: ResolveState): Boolean {
        if (element is TolkNamedElement) {
            val elementName = element.name ?: return true
            if (elementName == name) {
                return super.execute(element, state)
            }
        }
        return true
    }
}
