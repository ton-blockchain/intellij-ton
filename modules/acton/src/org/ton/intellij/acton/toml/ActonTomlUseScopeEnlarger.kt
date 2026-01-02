package org.ton.intellij.acton.toml

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.UseScopeEnlarger
import org.toml.lang.psi.TomlKeySegment

class ActonTomlUseScopeEnlarger : UseScopeEnlarger() {
    override fun getAdditionalUseScope(element: PsiElement): SearchScope? {
        if (element is TomlKeySegment && element.containingFile.name == "Acton.toml") {
            return GlobalSearchScope.projectScope(element.project)
        }
        return null
    }
}
