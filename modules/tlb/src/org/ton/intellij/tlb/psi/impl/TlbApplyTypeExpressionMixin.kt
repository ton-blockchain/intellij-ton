package org.ton.intellij.tlb.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tlb.psi.TlbApplyTypeExpression

abstract class TlbApplyTypeExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TlbApplyTypeExpression {

}