package org.ton.intellij.func.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.func.psi.FuncNamedElement
import org.ton.intellij.func.stub.FuncNamedStub

abstract class FuncNamedElementImpl<T : FuncNamedStub<*>> : FuncStubbedElementImpl<T>, FuncNamedElement {
    constructor(stub: T, nodeType: IStubElementType<*, *>) : super(stub, nodeType)
    constructor(node: ASTNode) : super(node)

    override fun setName(name: String): PsiElement {
        println("TODO: set name in func named element")
        return this
    }

    override fun getTextOffset(): Int = identifier?.textOffset ?: 0

    override fun getName(): String? = stub?.name ?: identifier?.text

    override fun getNameIdentifier(): PsiElement? = identifier
}
