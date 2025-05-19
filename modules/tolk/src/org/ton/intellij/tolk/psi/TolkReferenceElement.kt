package org.ton.intellij.tolk.psi

import com.intellij.psi.PsiElement

interface TolkReferenceElement : TolkElement {
    val referenceNameElement: PsiElement?

    val referenceName: String? get() = referenceNameElement?.text?.removeSurrounding("`")
}
