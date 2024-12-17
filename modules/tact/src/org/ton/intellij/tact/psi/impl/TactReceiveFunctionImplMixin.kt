package org.ton.intellij.tact.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tact.psi.TactBlock
import org.ton.intellij.tact.psi.TactInferenceContextOwner
import org.ton.intellij.tact.psi.TactReceiveFunction

abstract class TactReceiveFunctionImplMixin(
    node: ASTNode
) : ASTWrapperPsiElement(node), TactReceiveFunction, TactInferenceContextOwner {
    override val body: TactBlock? get() = block
}
