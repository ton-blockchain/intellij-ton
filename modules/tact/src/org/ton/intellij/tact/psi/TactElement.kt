package org.ton.intellij.tact.psi

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.UserDataHolderEx
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.CompositePsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.tree.IElementType

interface TactElement : PsiElement, UserDataHolderEx

abstract class TactElementImpl(type: IElementType) : CompositePsiElement(type), TactElement {
    override fun toString(): String = "${javaClass.simpleName}($elementType)"
}

abstract class TactStubbedElementImpl<StubT : StubBase<*>> : StubBasedPsiElementBase<StubT>, TactElement {
    constructor(node: ASTNode) : super(node)

    constructor(stub: StubT, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun toString(): String = "${javaClass.simpleName}($elementType)"
}
