package org.ton.intellij.tolk.stub

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import org.ton.intellij.tolk.psi.TolkSelfParameter
import org.ton.intellij.tolk.psi.impl.TolkSelfParameterImpl
import org.ton.intellij.tolk.stub.type.TolkStubElementType

class TolkSelfParameterStub(
    parent: StubElement<*>?,
    elementType: IStubElementType<*, *>,
    val isMutable: Boolean,
) : StubBase<TolkSelfParameter>(parent, elementType) {

    object Type : TolkStubElementType<TolkSelfParameterStub, TolkSelfParameter>(
        "SELF_PARAMETER",
    ) {
        override fun createPsi(stub: TolkSelfParameterStub) = TolkSelfParameterImpl(stub, this)

        override fun createStub(
            psi: TolkSelfParameter,
            parentStub: StubElement<out PsiElement?>?
        )=  TolkSelfParameterStub(parentStub, this, psi.isMutable)

        override fun serialize(
            stub: TolkSelfParameterStub,
            dataStream: StubOutputStream
        ) {
            dataStream.writeBoolean(stub.isMutable)
        }

        override fun deserialize(
            dataStream: StubInputStream,
            parentStub: StubElement<*>?
        ): TolkSelfParameterStub {
            val isMutable = dataStream.readBoolean()
            return TolkSelfParameterStub(parentStub, this, isMutable)
        }
    }
}
