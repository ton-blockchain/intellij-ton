package org.ton.intellij.tolk.stub

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import org.ton.intellij.tolk.psi.TolkElement
import org.ton.intellij.tolk.stub.type.TolkStubElementType
import org.ton.intellij.util.shouldCreateStubIfParentIsStub

open class TolkPlaceholderStub<PsiT : TolkElement>(parent: StubElement<*>, elementType: IStubElementType<*, *>) : StubBase<PsiT>(parent, elementType) {
    open class Type<PsiT : TolkElement>(
        debugName: String,
        private val psiCtor: (TolkPlaceholderStub<*>, IStubElementType<*, *>) -> PsiT
    ) :  TolkStubElementType<TolkPlaceholderStub<PsiT>, PsiT>(debugName) {
        override fun shouldCreateStub(node: ASTNode): Boolean = node.shouldCreateStubIfParentIsStub()

        override fun createPsi(stub: TolkPlaceholderStub<PsiT>): PsiT = psiCtor(stub, this)

        override fun createStub(
            psi: PsiT,
            parentStub: StubElement<out PsiElement?>
        ): TolkPlaceholderStub<PsiT> = TolkPlaceholderStub(parentStub, this)

        override fun serialize(
            stub: TolkPlaceholderStub<PsiT>,
            dataStream: StubOutputStream
        ) {
        }

        override fun deserialize(
            dataStream: StubInputStream,
            parentStub: StubElement<*>
        ): TolkPlaceholderStub<PsiT> = TolkPlaceholderStub(parentStub, this)
    }
}
