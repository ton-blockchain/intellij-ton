package org.ton.intellij.tolk.stub

import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.ton.intellij.tolk.psi.TolkStringLiteral
import org.ton.intellij.tolk.psi.impl.TolkStringLiteralImpl
import org.ton.intellij.tolk.stub.type.TolkStubElementType

class TolkStringLiteralStub : StubWithText<TolkStringLiteral> {
    constructor(
        parent: StubElement<*>?,
        elementType: IStubElementType<*, *>?,
        ref: StringRef?,
    ) : super(parent, elementType, ref)

    constructor(
        parent: StubElement<*>?,
        elementType: IStubElementType<*, *>?, text: String?,
    ) : this(parent, elementType, StringRef.fromString(text))

    class Type(name: String) : TolkStubElementType<TolkStringLiteralStub, TolkStringLiteral>(name) {
        override fun createPsi(stub: TolkStringLiteralStub): TolkStringLiteral {
            return TolkStringLiteralImpl(stub, this)
        }

        override fun createStub(psi: TolkStringLiteral, parentStub: StubElement<*>?): TolkStringLiteralStub {
            return TolkStringLiteralStub(parentStub, this, psi.text)
        }

        override fun serialize(stub: TolkStringLiteralStub, dataStream: StubOutputStream) {
            dataStream.writeName(stub.getText())
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): TolkStringLiteralStub {
            return TolkStringLiteralStub(parentStub, this, dataStream.readName())
        }
    }
}
