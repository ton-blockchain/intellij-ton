package org.ton.intellij.tact.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.ton.intellij.tact.psi.TactFunctionParameter
import org.ton.intellij.tact.psi.TactPsiFactory

abstract class TactFunctionParameterImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), TactFunctionParameter {
    override fun getName(): String? = identifier.text

    override fun setName(name: String): PsiElement {
        identifier.replace(TactPsiFactory(project).createIdentifier(name))
        return this
    }

    override fun getTextOffset(): Int = identifier.textOffset
}
