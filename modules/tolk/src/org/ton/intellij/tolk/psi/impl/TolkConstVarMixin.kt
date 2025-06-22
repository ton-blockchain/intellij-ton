package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.doc.psi.TolkDocComment
import org.ton.intellij.tolk.psi.TolkConstVar
import org.ton.intellij.tolk.stub.TolkConstVarStub
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.tolk.type.inference
import org.ton.intellij.util.childOfType
import javax.swing.Icon

abstract class TolkConstVarMixin : TolkNamedElementImpl<TolkConstVarStub>, TolkConstVar {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TolkConstVarStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override val type: TolkTy?
        get() = typeExpression?.type ?: inference?.getType(this)

    override val doc: TolkDocComment?
        get() = childOfType()

    override fun getIcon(flags: Int): Icon = TolkIcons.CONSTANT
}
