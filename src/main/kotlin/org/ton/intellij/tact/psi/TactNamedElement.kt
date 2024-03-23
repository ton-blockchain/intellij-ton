package org.ton.intellij.tact.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tact.psi.TactElementTypes.IDENTIFIER
import org.ton.intellij.tact.stub.TactNamedStub

interface TactNamedElement : TactElement, PsiNamedElement, NavigatablePsiElement

interface TactNameIdentifierOwner : TactNamedElement, PsiNameIdentifierOwner

abstract class TactNamedElementImpl<T : TactNamedStub<*>> : TactStubbedElementImpl<T>, TactNameIdentifierOwner {
    constructor(node: ASTNode) : super(node)
    constructor(stub: T, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun getNameIdentifier(): PsiElement? = findChildByType(IDENTIFIER)

    override fun getName(): String? = greenStub?.name ?: nameIdentifier?.text

    override fun setName(name: String): PsiElement {
        nameIdentifier?.replace(TactPsiFactory(project).createIdentifier(name))
        return this
    }

    override fun getTextOffset(): Int = nameIdentifier?.textOffset ?: super.getTextOffset()
}
