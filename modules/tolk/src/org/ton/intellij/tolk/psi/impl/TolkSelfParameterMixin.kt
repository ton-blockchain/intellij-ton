package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.parentOfType
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkSelfParameter
import org.ton.intellij.tolk.stub.TolkSelfParameterStub
import org.ton.intellij.tolk.type.TolkTy

abstract class TolkSelfParameterMixin : StubBasedPsiElementBase<TolkSelfParameterStub>, TolkSelfParameter {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TolkSelfParameterStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override val type: TolkTy?
        get() = parentOfType<TolkFunction>()?.receiverTy

    override fun getNameIdentifier(): PsiElement = selfTypeExpression

    override val identifier: PsiElement? get() = selfTypeExpression

    override val isMutable: Boolean get() = greenStub?.isMutable ?: (mutateKeyword != null)

    override val isDeprecated: Boolean = false

    override fun getName(): String = "self"

    override val rawName: String? = "self"

    override fun setName(name: String): PsiElement? {
        // can't rename self
        throw UnsupportedOperationException()
    }
}
