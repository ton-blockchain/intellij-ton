package org.ton.intellij.tact.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tact.psi.TactNamedElementImpl
import org.ton.intellij.tact.psi.TactTrait
import org.ton.intellij.tact.stub.TactTraitStub
import org.ton.intellij.tact.type.TactTy
import org.ton.intellij.tact.type.TactTyAdt

abstract class TactTraitImplMixin : TactNamedElementImpl<TactTraitStub>, TactTrait {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TactTraitStub, type: IStubElementType<*, *>) : super(stub, type)

    override val declaredType: TactTy
        get() = TactTyAdt(this)
}
