package org.ton.intellij.tlb.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tlb.TlbSize
import org.ton.intellij.tlb.psi.TlbConstraintTypeExpression

abstract class TlbConstraintTypeExpressionMixin(node: ASTNode): ASTWrapperPsiElement(node), TlbConstraintTypeExpression {
    override val tlbSize: TlbSize get() = TlbSize.ZERO
}