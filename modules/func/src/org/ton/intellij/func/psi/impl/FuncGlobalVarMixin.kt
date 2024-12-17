package org.ton.intellij.func.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.func.FuncIcons
import org.ton.intellij.func.psi.FuncGlobalVar
import org.ton.intellij.func.stub.FuncGlobalVarStub
import javax.swing.Icon

abstract class FuncGlobalVarMixin : FuncNamedElementImpl<FuncGlobalVarStub>, FuncGlobalVar {
    constructor(node: ASTNode) : super(node)

    constructor(stub: FuncGlobalVarStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun getIcon(flags: Int): Icon? {
        return FuncIcons.GLOBAL_VARIABLE
    }
}
