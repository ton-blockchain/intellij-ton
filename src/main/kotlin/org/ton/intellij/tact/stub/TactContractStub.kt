package org.ton.intellij.tact.stub

import com.intellij.psi.stubs.*
import com.intellij.util.io.StringRef
import org.ton.intellij.tact.psi.TactContract
import org.ton.intellij.tact.psi.impl.TactContractImpl
import org.ton.intellij.tact.stub.index.indexContract
import org.ton.intellij.tact.stub.index.indexMessage
import org.ton.intellij.tact.stub.index.indexStruct

class TactContractStub(
    parent: StubElement<*>,
    elementType: IStubElementType<*, *>,
    name: StringRef?,
) : TactNamedStub<TactContract>(parent, elementType, name) {
    constructor(parent: StubElement<*>, elementType: IStubElementType<*, *>, name: String?) : this(
        parent,
        elementType,
        StringRef.fromString(name),
    )

    override fun toString(): String {
        return "${javaClass.simpleName}(name=$name)"
    }

    object Type : TactStubElementType<TactContractStub, TactContract>("CONTRACT") {
        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): TactContractStub {
            return TactContractStub(parentStub, this, dataStream.readName())
        }

        override fun serialize(stub: TactContractStub, dataStream: StubOutputStream) {
            dataStream.writeName(stub.name)
        }

        override fun createPsi(stub: TactContractStub): TactContract {
            return TactContractImpl(stub, this)
        }

        override fun createStub(psi: TactContract, parentStub: StubElement<*>): TactContractStub {
            return TactContractStub(parentStub, this, psi.name)
        }

        override fun indexStub(stub: TactContractStub, sink: IndexSink) = sink.indexContract(stub)
    }
}
