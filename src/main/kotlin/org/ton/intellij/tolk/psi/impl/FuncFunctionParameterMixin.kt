package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tolk.psi.TolkFunctionParameter
import org.ton.intellij.tolk.stub.TolkFunctionParameterStub
import org.ton.intellij.tact.TactIcons
import javax.swing.Icon

abstract class TolkFunctionParameterMixin : TolkNamedElementImpl<TolkFunctionParameterStub>, TolkFunctionParameter {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TolkFunctionParameterStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun getIcon(flags: Int): Icon = TactIcons.PARAMETER
}
