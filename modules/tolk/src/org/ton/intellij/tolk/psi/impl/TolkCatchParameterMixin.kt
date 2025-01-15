package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.psi.TolkCatch
import org.ton.intellij.tolk.psi.TolkCatchParameter
import org.ton.intellij.tolk.psi.TolkPsiFactory
import org.ton.intellij.tolk.type.TolkType

abstract class TolkCatchParameterMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkCatchParameter {
    override val type: TolkType?
        get() = if ((parent as? TolkCatch)?.catchParameterList?.indexOf(this) == 0) {
            TolkType.Int
        } else {
            null
        }

    override fun getName(): String = identifier.text

    override fun setName(name: String): PsiElement {
        identifier.replace(TolkPsiFactory[project].createIdentifier(name))
        return this
    }

    override fun getNameIdentifier(): PsiElement = identifier

    override fun getTextOffset(): Int = identifier.textOffset
}