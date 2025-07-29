package org.ton.intellij.func.psi

import com.intellij.navigation.NavigationItem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner

interface FuncNamedElement : PsiNameIdentifierOwner, FuncElement, NavigationItem {
    val identifier: PsiElement?
}
