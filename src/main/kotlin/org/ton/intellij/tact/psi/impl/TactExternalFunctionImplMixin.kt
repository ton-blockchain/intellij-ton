package org.ton.intellij.tact.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tact.psi.TactBlock
import org.ton.intellij.tact.psi.TactExternalFunction
import org.ton.intellij.tact.psi.TactInferenceContextOwner
import org.ton.intellij.tact.psi.TactReceiveFunction

abstract class TactExternalFunctionImplMixin(
    node: ASTNode
) : ASTWrapperPsiElement(node), TactExternalFunction, TactInferenceContextOwner {
    override val body: TactBlock? get() = block
}
