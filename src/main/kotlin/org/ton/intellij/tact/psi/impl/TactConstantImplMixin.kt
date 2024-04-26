package org.ton.intellij.tact.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tact.TactIcons
import org.ton.intellij.tact.psi.TactConstant
import org.ton.intellij.tact.psi.TactNamedElementImpl
import org.ton.intellij.tact.stub.TactConstantStub
import javax.swing.Icon

abstract class TactConstantImplMixin : TactNamedElementImpl<TactConstantStub>, TactConstant {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TactConstantStub, type: IStubElementType<*, *>) : super(stub, type)

    override val isAbstract: Boolean
        get() = greenStub?.isAbstract ?: constantAttributeList.any { it.abstractKeyword != null }

    override fun getReference(): PsiReference? = super.getReference()

    override fun getIcon(flags: Int): Icon = TactIcons.CONSTANT
}
