package org.ton.intellij.tolk.stub

import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.ton.intellij.tolk.psi.TolkTypeParameterList
import org.ton.intellij.tolk.psi.impl.TolkTypeParameterListImpl
import org.ton.intellij.tolk.stub.type.TolkStubElementType

class TolkTypeParameterListStub : StubWithText<TolkTypeParameterList> {
    constructor(
        parent: StubElement<*>?,
        elementType: IStubElementType<*, *>?,
        ref: StringRef?,
    ) : super(parent, elementType, ref)

    constructor(
        parent: StubElement<*>?,
        elementType: IStubElementType<*, *>?, text: String?,
    ) : this(parent, elementType, StringRef.fromString(text))

    class Type(name: String) : TolkStubElementType<TolkTypeParameterListStub, TolkTypeParameterList>(name) {
        override fun createPsi(stub: TolkTypeParameterListStub): TolkTypeParameterList {
            return TolkTypeParameterListImpl(stub, this)
        }

        override fun createStub(psi: TolkTypeParameterList, parentStub: StubElement<*>?): TolkTypeParameterListStub {
            return TolkTypeParameterListStub(parentStub, this, psi.text)
        }

        override fun serialize(stub: TolkTypeParameterListStub, dataStream: StubOutputStream) {
            dataStream.writeName(stub.getText())
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): TolkTypeParameterListStub {
            return TolkTypeParameterListStub(parentStub, this, dataStream.readName())
        }
    }
}
