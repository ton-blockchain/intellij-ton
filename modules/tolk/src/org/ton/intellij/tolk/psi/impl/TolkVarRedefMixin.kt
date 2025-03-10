package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import org.ton.intellij.tolk.psi.TolkVarRedef
import org.ton.intellij.tolk.type.TolkType

abstract class TolkVarRedefMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkVarRedef {
    override val type: TolkType?
        get() = referenceExpression.type
}
