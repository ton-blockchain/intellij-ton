package org.ton.intellij.tolk.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner

interface TolkNamedElement : PsiNameIdentifierOwner, TolkElement {
    val identifier: PsiElement?
}
