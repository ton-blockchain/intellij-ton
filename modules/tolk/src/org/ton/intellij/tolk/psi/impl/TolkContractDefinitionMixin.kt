package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tolk.acton.TolkContractDefinitionReference
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.stub.TolkContractDefinitionStub

abstract class TolkContractDefinitionMixin :
    TolkNamedElementImpl<TolkContractDefinitionStub>,
    TolkContractDefinition,
    TolkInferenceContextOwner {
    constructor(node: ASTNode) : super(node)
    constructor(stub: TolkContractDefinitionStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun getReference(): PsiReference? {
        val reference = TolkContractDefinitionReference(this)
        return reference.takeIf { it.resolve() != null }
    }

    override fun getReferences(): Array<PsiReference> = reference?.let { arrayOf(it) } ?: PsiReference.EMPTY_ARRAY
}
