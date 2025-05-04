package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.findParentOfType
import org.ton.intellij.tolk.psi.TolkBlockStatement
import org.ton.intellij.tolk.psi.TolkPsiFactory
import org.ton.intellij.tolk.psi.TolkVar
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.tolk.type.inference

abstract class TolkVarMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkVar {
    override fun getName(): String = identifier.text

    override fun setName(name: String): PsiElement {
        identifier.replace(TolkPsiFactory[project].createIdentifier(name))
        return this
    }

    override fun getNameIdentifier(): PsiElement = identifier

    override fun getTextOffset(): Int = identifier.textOffset

    override val type: TolkTy?
        get() = typeExpression?.type ?: inference?.getType(this)

    override fun getContext(): PsiElement? {
        return findParentOfType<TolkBlockStatement>()
    }

    override fun toString(): String = "TolkVar($text)"
}
