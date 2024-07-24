package org.ton.intellij.tlb.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tlb.stub.TlbNamedStub
import org.ton.intellij.tlb.stub.TlbStubbedElementImpl

abstract class TlbNamedElementImpl<T : TlbNamedStub<*>> : TlbStubbedElementImpl<T>, TlbNamedElement {
    constructor(node: ASTNode) : super(node)
    constructor(stub: T, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun setName(name: String): PsiElement {
        identifier?.replace(TlbPsiFactory[project].createIdentifier(name))
        return this
    }

    override fun getTextOffset(): Int = identifier?.textOffset ?: 0

    override fun getName(): String? = stub?.name ?: identifier?.text

    override fun getNameIdentifier(): PsiElement? = identifier
}
