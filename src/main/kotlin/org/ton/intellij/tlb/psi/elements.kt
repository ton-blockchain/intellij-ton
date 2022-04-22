package org.ton.intellij.tlb.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import org.ton.intellij.tlb.resolve.TlbNamedRefReference

interface TlbElement : PsiElement
abstract class TlbElementImpl(node: ASTNode) : ASTWrapperPsiElement(node)

interface TlbNamedElement : TlbElement, PsiNameIdentifierOwner

abstract class TlbNamedElementImpl(node: ASTNode) : TlbElementImpl(node), TlbNamedElement {
    override fun getNameIdentifier(): PsiElement? = findChildByType(TlbTypes.IDENTIFIER)
    override fun getName(): String? = nameIdentifier?.text
    override fun setName(name: String): PsiElement = apply {
        TODO()
//        nameIdentifier?.replace(project.TlbPsiFactory.createIdentifier(name))
    }

    override fun getTextOffset(): Int = nameIdentifier?.textOffset ?: super.getTextOffset()
}

abstract class TlbNamedRefMixin(node: ASTNode) : TlbNamedElementImpl(node), TlbNamedRef {
    override fun getReference() = TlbNamedRefReference(this)
}