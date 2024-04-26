package org.ton.intellij.tact.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tact.psi.TactField
import org.ton.intellij.tact.psi.TactNamedElementImpl
import org.ton.intellij.tact.stub.TactFieldStub

abstract class TactFieldImplMixin : TactNamedElementImpl<TactFieldStub>, TactField {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TactFieldStub, type: IStubElementType<*, *>) : super(stub, type)

    override fun getReference(): PsiReference? {
        return super.getReference()
    }

    override fun toString(): String {
        return "TactField(name=$name)"
    }
}
