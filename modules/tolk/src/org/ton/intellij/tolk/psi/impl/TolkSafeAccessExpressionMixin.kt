package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkSafeAccessExpression

abstract class TolkSafeAccessExpressionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkSafeAccessExpression {
}
