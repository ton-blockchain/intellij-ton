package org.ton.intellij.tolk.stub

import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.ton.intellij.tolk.psi.TolkFunctionReceiver
import org.ton.intellij.tolk.psi.impl.TolkFunctionReceiverImpl
import org.ton.intellij.tolk.stub.type.TolkStubElementType

class TolkFunctionReceiverStub : StubWithText<TolkFunctionReceiver> {
    constructor(
        parent: StubElement<*>?,
        elementType: IStubElementType<*, *>?,
        ref: StringRef?,
    ) : super(parent, elementType, ref)

    constructor(
        parent: StubElement<*>?,
        elementType: IStubElementType<*, *>?, text: String?,
    ) : this(parent, elementType, StringRef.fromString(text))

    class Type(name: String) : TolkStubElementType<TolkFunctionReceiverStub, TolkFunctionReceiver>(name) {
        override fun createPsi(stub: TolkFunctionReceiverStub): TolkFunctionReceiver {
            return TolkFunctionReceiverImpl(stub, this)
        }

        override fun createStub(psi: TolkFunctionReceiver, parentStub: StubElement<*>?): TolkFunctionReceiverStub {
            return TolkFunctionReceiverStub(parentStub, this, psi.text)
        }

        override fun serialize(stub: TolkFunctionReceiverStub, dataStream: StubOutputStream) {
            dataStream.writeName(stub.getText())
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): TolkFunctionReceiverStub {
            return TolkFunctionReceiverStub(parentStub, this, dataStream.readName())
        }
    }
}
