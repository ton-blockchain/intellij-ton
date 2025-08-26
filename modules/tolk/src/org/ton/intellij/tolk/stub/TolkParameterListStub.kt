package org.ton.intellij.tolk.stub

import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.ton.intellij.tolk.psi.TolkParameterList
import org.ton.intellij.tolk.psi.impl.TolkParameterListImpl
import org.ton.intellij.tolk.stub.type.TolkStubElementType

class TolkParameterListStub : StubWithText<TolkParameterList> {
    constructor(
        parent: StubElement<*>?,
        elementType: IStubElementType<*, *>?,
        ref: StringRef?,
    ) : super(parent, elementType, ref)

    constructor(
        parent: StubElement<*>?,
        elementType: IStubElementType<*, *>?, text: String?,
    ) : this(parent, elementType, StringRef.fromString(text))

    class Type(name: String) : TolkStubElementType<TolkParameterListStub, TolkParameterList>(name) {
        override fun createPsi(stub: TolkParameterListStub): TolkParameterList {
            return TolkParameterListImpl(stub, this)
        }

        override fun createStub(psi: TolkParameterList, parentStub: StubElement<*>?): TolkParameterListStub {
            return TolkParameterListStub(parentStub, this, psi.text)
        }

        override fun serialize(stub: TolkParameterListStub, dataStream: StubOutputStream) {
            dataStream.writeName(stub.getText())
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): TolkParameterListStub {
            return TolkParameterListStub(parentStub, this, dataStream.readName())
        }
    }
}
