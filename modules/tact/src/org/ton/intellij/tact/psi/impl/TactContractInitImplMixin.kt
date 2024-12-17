package org.ton.intellij.tact.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tact.psi.TactBlock
import org.ton.intellij.tact.psi.TactContractInit
import org.ton.intellij.tact.psi.TactInferenceContextOwner

abstract class TactContractInitImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), TactContractInit,
    TactInferenceContextOwner {
    override val body: TactBlock? get() = block
}
