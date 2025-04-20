package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.search.SearchScope
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.psi.TolkStruct
import org.ton.intellij.tolk.stub.TolkStructStub
import org.ton.intellij.tolk.type.TolkStructType
import org.ton.intellij.tolk.type.TolkType
import javax.swing.Icon

abstract class TolkStructMixin : TolkNamedElementImpl<TolkStructStub>, TolkStruct {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TolkStructStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun getUseScope(): SearchScope {
        return super.getUseScope()
    }

    override val type: TolkStructType = TolkType.struct(this)

    override fun getBaseIcon() = TolkIcons.STRUCTURE

    override fun getIcon(flags: Int): Icon? = getBaseIcon()
}
