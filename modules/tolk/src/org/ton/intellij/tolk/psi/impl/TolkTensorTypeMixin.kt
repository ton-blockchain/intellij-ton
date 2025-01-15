package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkTensorType
import org.ton.intellij.tolk.type.TolkType

abstract class TolkTensorTypeMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkTensorType {
    override val type: TolkType?
        get() = TolkType.create(typeExpressionList.map { it.type ?: return null })
}