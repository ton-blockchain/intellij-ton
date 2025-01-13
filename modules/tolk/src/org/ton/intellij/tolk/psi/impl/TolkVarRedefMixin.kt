package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.psi.TolkPsiFactory
import org.ton.intellij.tolk.psi.TolkVar
import org.ton.intellij.tolk.psi.TolkVarRedef
import org.ton.intellij.tolk.type.TolkType
import org.ton.intellij.tolk.type.infer.inference

abstract class TolkVarRedefMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkVarRedef {
    override val type: TolkType?
        get() = referenceExpression.type
}
