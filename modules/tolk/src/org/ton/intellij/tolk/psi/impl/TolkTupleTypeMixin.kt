package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkTupleType
import org.ton.intellij.tolk.type.TolkType

abstract class TolkTupleTypeMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkTupleType {
    override val type: TolkType?
        get() = TolkType.TypedTuple(typeExpressionList.map { it.type ?: return null })
}