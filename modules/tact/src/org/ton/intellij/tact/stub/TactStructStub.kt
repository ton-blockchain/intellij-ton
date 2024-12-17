package org.ton.intellij.tact.stub

import com.intellij.psi.stubs.*
import com.intellij.util.io.StringRef
import org.ton.intellij.tact.psi.TactStruct
import org.ton.intellij.tact.psi.impl.TactStructImpl
import org.ton.intellij.tact.stub.index.indexStruct

class TactStructStub(
    parent: StubElement<*>,
    elementType: IStubElementType<*, *>,
    name: StringRef?,
) : TactNamedStub<TactStruct>(parent, elementType, name) {
    constructor(parent: StubElement<*>, elementType: IStubElementType<*, *>, name: String?) : this(
        parent,
        elementType,
        StringRef.fromString(name),
    )

    object Type : TactStubElementType<TactStructStub, TactStruct>("STRUCT") {
        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): TactStructStub {
            return TactStructStub(parentStub, this, dataStream.readName())
        }

        override fun serialize(stub: TactStructStub, dataStream: StubOutputStream) {
            dataStream.writeName(stub.name)
        }

        override fun createPsi(stub: TactStructStub): TactStruct {
            return TactStructImpl(stub, this)
        }

        override fun createStub(psi: TactStruct, parentStub: StubElement<*>): TactStructStub {
            return TactStructStub(parentStub, this, psi.name)
        }

        override fun indexStub(stub: TactStructStub, sink: IndexSink) = sink.indexStruct(stub)
    }
}
