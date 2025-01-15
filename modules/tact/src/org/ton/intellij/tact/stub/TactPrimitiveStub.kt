package org.ton.intellij.tact.stub

import com.intellij.psi.stubs.*
import com.intellij.util.io.StringRef
import org.ton.intellij.tact.psi.TactPrimitive
import org.ton.intellij.tact.psi.impl.TactPrimitiveImpl
import org.ton.intellij.tact.stub.index.indexPrimitive

class TactPrimitiveStub(
    parent: StubElement<*>,
    elementType: IStubElementType<*, *>,
    name: StringRef?,
) : TactNamedStub<TactPrimitive>(parent, elementType, name) {
    constructor(parent: StubElement<*>, elementType: IStubElementType<*, *>, name: String?) : this(
        parent,
        elementType,
        StringRef.fromString(name),
    )

    override fun toString(): String {
        return "${javaClass.simpleName}(name=$name)"
    }

    object Type : TactStubElementType<TactPrimitiveStub, TactPrimitive>("PRIMITIVE") {
        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): TactPrimitiveStub {
            return TactPrimitiveStub(parentStub, this, dataStream.readName())
        }

        override fun serialize(stub: TactPrimitiveStub, dataStream: StubOutputStream) {
            dataStream.writeName(stub.name)
        }

        override fun createPsi(stub: TactPrimitiveStub): TactPrimitive {
            return TactPrimitiveImpl(stub, this)
        }

        override fun createStub(psi: TactPrimitive, parentStub: StubElement<*>): TactPrimitiveStub {
            return TactPrimitiveStub(parentStub, this, psi.name)
        }

        override fun indexStub(stub: TactPrimitiveStub, sink: IndexSink) = sink.indexPrimitive(stub)
    }
}
