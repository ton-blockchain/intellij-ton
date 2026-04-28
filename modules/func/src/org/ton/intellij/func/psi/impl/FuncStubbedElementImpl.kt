@file:Suppress("DEPRECATION")

package org.ton.intellij.func.psi.impl

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubBase
import org.ton.intellij.func.psi.FuncElement
import org.ton.intellij.util.greenStub

abstract class FuncStubbedElementImpl<T : StubBase<*>> :
    StubBasedPsiElementBase<T>,
    FuncElement {
    constructor(stub: T, nodeType: IStubElementType<*, *>) : super(stub, nodeType)
    constructor(node: ASTNode) : super(node)

    override fun toString(): String = (greenStub?.stubType ?: node.elementType).toString()
}
