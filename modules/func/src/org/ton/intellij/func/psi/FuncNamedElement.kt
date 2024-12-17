package org.ton.intellij.func.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner

interface FuncNamedElement : PsiNameIdentifierOwner, FuncElement {
    val identifier: PsiElement?
}
