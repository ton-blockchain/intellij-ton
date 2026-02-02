package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.type.TolkTy

abstract class TolkContractFieldMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkContractField, TolkTypedElement, TolkNamedElement {
    override fun getNameIdentifier(): PsiElement? = identifier

    override val rawName: String?
        get() = name

    override val type: TolkTy?
        get() = expression?.type

    override val isDeprecated: Boolean
        get() = false

    override fun getName(): String? = identifier.text?.removeSurrounding("`")

    override fun setName(name: String): PsiElement {
        identifier.replace(TolkPsiFactory[project].createIdentifier(name))
        return this
    }
}
