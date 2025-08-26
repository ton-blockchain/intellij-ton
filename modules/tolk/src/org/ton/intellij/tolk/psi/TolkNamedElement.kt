package org.ton.intellij.tolk.psi

import com.intellij.navigation.NavigationItem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner

interface TolkNamedElement : PsiNameIdentifierOwner, TolkElement, NavigationItem {
    val identifier: PsiElement?
    val rawName: String?
    val isDeprecated: Boolean
}
