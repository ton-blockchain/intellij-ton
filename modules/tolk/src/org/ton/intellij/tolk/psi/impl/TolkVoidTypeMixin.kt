package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkVoidType
import org.ton.intellij.tolk.type.TolkType

abstract class TolkVoidTypeMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkVoidType {
    override val type: TolkType get() = TolkType.Unit
}