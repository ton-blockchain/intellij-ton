package org.ton.intellij.tact.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tact.psi.TactBlock
import org.ton.intellij.tact.psi.TactBouncedFunction
import org.ton.intellij.tact.psi.TactInferenceContextOwner

abstract class TactBouncedFunctionImplMixin(
    node: ASTNode
) : ASTWrapperPsiElement(node), TactBouncedFunction, TactInferenceContextOwner {
    override val body: TactBlock? get() = block
}
