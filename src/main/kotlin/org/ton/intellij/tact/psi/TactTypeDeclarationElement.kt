package org.ton.intellij.tact.psi

import org.ton.intellij.tact.type.TactTy

interface TactTypeDeclarationElement : TactNamedElement {
    val declaredType: TactTy
}
