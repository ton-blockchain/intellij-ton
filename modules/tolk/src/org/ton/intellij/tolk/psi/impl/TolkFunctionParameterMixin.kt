package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.psi.TolkParameter
import org.ton.intellij.tolk.stub.TolkParameterStub
import org.ton.intellij.tolk.type.TolkType
import javax.swing.Icon

abstract class TolkParameterMixin : TolkNamedElementImpl<TolkParameterStub>, TolkParameter {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TolkParameterStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun getIcon(flags: Int): Icon = TolkIcons.PARAMETER

    override val type: TolkType? get() = typeExpression?.type

    val isMutable: Boolean get() = stub?.isMutable ?: (mutateKeyword != null)

    override fun getPresentation(): ItemPresentation? {
        return super.getPresentation()
    }
}

val TolkParameter.isMutable: Boolean get() =
    (this as? TolkParameterMixin)?.isMutable ?: (mutateKeyword != null)
