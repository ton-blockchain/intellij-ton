package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.psi.*

abstract class TolkContractDefinitionMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkContractDefinition, TolkInferenceContextOwner, TolkNamedElement {
    override val identifier: PsiElement?
        get() = findChildByType(TolkElementTypes.IDENTIFIER)

    override fun getName(): String? = identifier?.text?.removeSurrounding("`")

    override fun setName(name: String): PsiElement {
        identifier?.replace(TolkPsiFactory[project].createIdentifier(name))
        return this
    }

    override fun getNameIdentifier(): PsiElement? = identifier

    override val rawName: String?
        get() = identifier?.text

    override val isDeprecated: Boolean
        get() = false

    override fun getTextOffset(): Int = identifier?.textOffset ?: super.getTextOffset()
}
