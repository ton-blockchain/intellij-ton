package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.psi.TolkGlobalVar
import org.ton.intellij.tolk.stub.TolkGlobalVarStub
import javax.swing.Icon

abstract class TolkGlobalVarMixin : TolkNamedElementImpl<TolkGlobalVarStub>, TolkGlobalVar {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TolkGlobalVarStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun getIcon(flags: Int): Icon? {
        return TolkIcons.GLOBAL_VARIABLE
    }
}
