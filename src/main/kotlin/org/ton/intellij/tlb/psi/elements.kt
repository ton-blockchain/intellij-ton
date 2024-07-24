package org.ton.intellij.tlb.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner

interface TlbElement : PsiElement

interface TlbNamedElement : TlbElement, PsiNameIdentifierOwner {
    val identifier: PsiElement?
}
