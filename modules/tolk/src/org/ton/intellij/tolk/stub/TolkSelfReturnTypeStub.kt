package org.ton.intellij.tolk.stub

import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.ton.intellij.tolk.psi.TolkSelfReturnType
import org.ton.intellij.tolk.psi.impl.TolkSelfReturnTypeImpl
import org.ton.intellij.tolk.stub.type.TolkStubElementType

class TolkSelfReturnTypeStub : StubWithText<TolkSelfReturnType> {
    constructor(
        parent: StubElement<*>?,
        elementType: IStubElementType<*, *>?,
        ref: StringRef?,
    ) : super(parent, elementType, ref)

    constructor(
        parent: StubElement<*>?,
        elementType: IStubElementType<*, *>?, text: String?,
    ) : this(parent, elementType, StringRef.fromString(text))

    class Type(name: String) : TolkStubElementType<TolkSelfReturnTypeStub, TolkSelfReturnType>(name) {
        override fun createPsi(stub: TolkSelfReturnTypeStub): TolkSelfReturnType {
            return TolkSelfReturnTypeImpl(stub, this)
        }

        override fun createStub(psi: TolkSelfReturnType, parentStub: StubElement<*>?): TolkSelfReturnTypeStub {
            return TolkSelfReturnTypeStub(parentStub, this, psi.text)
        }

        override fun serialize(stub: TolkSelfReturnTypeStub, dataStream: StubOutputStream) {
            dataStream.writeName(stub.getText())
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): TolkSelfReturnTypeStub {
            return TolkSelfReturnTypeStub(parentStub, this, dataStream.readName())
        }
    }
}
