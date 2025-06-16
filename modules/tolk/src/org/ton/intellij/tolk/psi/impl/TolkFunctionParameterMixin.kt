package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.psi.TolkParameter
import org.ton.intellij.tolk.stub.TolkParameterStub
import org.ton.intellij.tolk.type.TolkTy
import javax.swing.Icon

abstract class TolkParameterMixin : TolkNamedElementImpl<TolkParameterStub>, TolkParameter {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TolkParameterStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun getIcon(flags: Int): Icon = TolkIcons.PARAMETER

    override val type: TolkTy?
        get() {
            val t = typeExpression.type
            return t
        }

    override val isMutable: Boolean get() = greenStub?.isMutable ?: (mutateKeyword != null)
}
