package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.psi.TolkCatchParameter
import org.ton.intellij.tolk.psi.TolkPsiFactory
import org.ton.intellij.tolk.type.TolkType
import org.ton.intellij.tolk.type.infer.inference

abstract class TolkCatchParameterMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkCatchParameter {
    override val type: TolkType? get() = inference?.getType(this)

    override fun getName(): String = identifier.text

    override fun setName(name: String): PsiElement {
        identifier.replace(TolkPsiFactory[project].createIdentifier(name))
        return this
    }

    override fun getNameIdentifier(): PsiElement = identifier

    override fun getTextOffset(): Int = identifier.textOffset
}