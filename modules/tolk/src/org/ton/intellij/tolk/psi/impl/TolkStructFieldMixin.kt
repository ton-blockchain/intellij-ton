package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tolk.psi.TolkStructField
import org.ton.intellij.tolk.stub.TolkStructFieldStub
import org.ton.intellij.tolk.type.TolkType

abstract class TolkStructFieldMixin : TolkNamedElementImpl<TolkStructFieldStub>, TolkStructField {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TolkStructFieldStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override val type: TolkType?
        get() = typeExpression?.type
}
