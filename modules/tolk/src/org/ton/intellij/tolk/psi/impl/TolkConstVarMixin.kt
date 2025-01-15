package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.psi.TolkConstVar
import org.ton.intellij.tolk.stub.TolkConstVarStub
import org.ton.intellij.tolk.type.TolkType
import javax.swing.Icon

abstract class TolkConstVarMixin : TolkNamedElementImpl<TolkConstVarStub>, TolkConstVar {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TolkConstVarStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override val type: TolkType?
        get() = typeExpression?.type ?: expression?.type

    override fun getIcon(flags: Int): Icon = TolkIcons.CONSTANT
}
