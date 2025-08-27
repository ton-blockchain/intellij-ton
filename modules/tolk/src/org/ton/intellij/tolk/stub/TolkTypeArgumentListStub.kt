package org.ton.intellij.tolk.stub

import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.ton.intellij.tolk.psi.TolkTypeArgumentList
import org.ton.intellij.tolk.psi.impl.TolkTypeArgumentListImpl
import org.ton.intellij.tolk.stub.type.TolkStubElementType

class TolkTypeArgumentListStub : StubWithText<TolkTypeArgumentList> {
    constructor(
        parent: StubElement<*>?,
        elementType: IStubElementType<*, *>?,
        ref: StringRef?,
    ) : super(parent, elementType, ref)

    constructor(
        parent: StubElement<*>?,
        elementType: IStubElementType<*, *>?, text: String?,
    ) : this(parent, elementType, StringRef.fromString(text))

    class Type(name: String) : TolkStubElementType<TolkTypeArgumentListStub, TolkTypeArgumentList>(name) {
        override fun createPsi(stub: TolkTypeArgumentListStub): TolkTypeArgumentList {
            return TolkTypeArgumentListImpl(stub, this)
        }

        override fun createStub(psi: TolkTypeArgumentList, parentStub: StubElement<*>?): TolkTypeArgumentListStub {
            return TolkTypeArgumentListStub(parentStub, this, psi.text)
        }

        override fun serialize(stub: TolkTypeArgumentListStub, dataStream: StubOutputStream) {
            dataStream.writeName(stub.getText())
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): TolkTypeArgumentListStub {
            return TolkTypeArgumentListStub(parentStub, this, dataStream.readName())
        }
    }
}
