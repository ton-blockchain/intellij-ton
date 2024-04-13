package org.ton.intellij.tact.stub

import com.intellij.psi.stubs.*
import com.intellij.util.io.StringRef
import org.ton.intellij.tact.psi.TactField
import org.ton.intellij.tact.psi.impl.TactFieldImpl
import org.ton.intellij.tact.stub.index.indexField
import org.ton.intellij.tact.stub.index.indexPrimitive

class TactFieldStub(
    parent: StubElement<*>,
    elementType: IStubElementType<*, *>,
    name: StringRef?,
) : TactNamedStub<TactField>(parent, elementType, name) {
    constructor(parent: StubElement<*>, elementType: IStubElementType<*, *>, name: String?) : this(
        parent,
        elementType,
        StringRef.fromString(name),
    )

    override fun toString(): String {
        return "${javaClass.simpleName}(name=$name)"
    }

    object Type : TactStubElementType<TactFieldStub, TactField>("FIELD") {
        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): TactFieldStub {
            return TactFieldStub(parentStub, this, dataStream.readName())
        }

        override fun serialize(stub: TactFieldStub, dataStream: StubOutputStream) {
            dataStream.writeName(stub.name)
        }

        override fun createPsi(stub: TactFieldStub): TactField {
            return TactFieldImpl(stub, this)
        }

        override fun createStub(psi: TactField, parentStub: StubElement<*>): TactFieldStub {
            return TactFieldStub(parentStub, this, psi.name)
        }

        override fun indexStub(stub: TactFieldStub, sink: IndexSink) = sink.indexField(stub)
    }
}
