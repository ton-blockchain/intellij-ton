package org.ton.intellij.fift.psi

import com.intellij.lang.ASTNode

abstract class FiftAssemblyInstructionMixin(node: ASTNode) : FiftNamedElementImpl(node), FiftOrdinaryWord {
    override fun getReference() = FiftAssemblyReference(this as FiftTvmInstruction)
}
