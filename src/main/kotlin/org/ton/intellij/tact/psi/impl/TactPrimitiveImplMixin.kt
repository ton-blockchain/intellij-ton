package org.ton.intellij.tact.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tact.TactIcons
import org.ton.intellij.tact.psi.TactNamedElement
import org.ton.intellij.tact.psi.TactNamedElementImpl
import org.ton.intellij.tact.psi.TactPrimitive
import org.ton.intellij.tact.stub.TactPrimitiveStub
import org.ton.intellij.tact.type.TactTyRef
import javax.swing.Icon

abstract class TactPrimitiveImplMixin : TactNamedElementImpl<TactPrimitiveStub>, TactPrimitive {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TactPrimitiveStub, type: IStubElementType<*, *>) : super(stub, type)

    override val declaredTy
        get() = TactTyRef(this)

    override val members: Sequence<TactNamedElement> get() = emptySequence()

    override fun getIcon(flags: Int): Icon = TactIcons.PRIMITIVE
}
