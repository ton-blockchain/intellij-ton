package org.ton.intellij.tact.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.ton.intellij.tact.TactIcons
import org.ton.intellij.tact.psi.TactCatchParameter
import org.ton.intellij.tact.psi.TactPsiFactory
import javax.swing.Icon

abstract class TactCatchParameterMixin(node: ASTNode) : ASTWrapperPsiElement(node), TactCatchParameter {
    override fun getName(): String = identifier.text

    override fun setName(name: String): PsiElement {
        (identifier).replace(TactPsiFactory(project).createIdentifier(name))
        return this
    }

    override fun getTextOffset(): Int = identifier.textOffset

    override fun getIcon(flags: Int): Icon = TactIcons.VARIABLE
}
