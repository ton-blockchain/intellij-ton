package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.psi.TolkConstVar
import org.ton.intellij.tolk.stub.TolkConstVarStub
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.tolk.type.inference
import javax.swing.Icon

abstract class TolkConstVarMixin : TolkNamedElementImpl<TolkConstVarStub>, TolkConstVar {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TolkConstVarStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override val type: TolkTy?
        get() = inference?.getType(this) ?: typeExpression?.type

    override fun getIcon(flags: Int): Icon = TolkIcons.CONSTANT
}
