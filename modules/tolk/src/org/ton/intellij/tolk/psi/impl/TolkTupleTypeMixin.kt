package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkTupleTypeExpression
import org.ton.intellij.tolk.type.TolkTy

abstract class TolkTupleTypeMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkTupleTypeExpression {
    override val type: TolkTy?
        get() = TolkTy.typedTuple(typeExpressionList.map { it.type ?: return null })
}
