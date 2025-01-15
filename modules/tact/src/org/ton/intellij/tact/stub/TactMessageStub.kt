package org.ton.intellij.tact.stub

import com.intellij.psi.stubs.*
import com.intellij.util.io.StringRef
import org.ton.intellij.tact.psi.TactMessage
import org.ton.intellij.tact.psi.impl.TactMessageImpl
import org.ton.intellij.tact.stub.index.indexMessage

class TactMessageStub(
    parent: StubElement<*>,
    elementType: IStubElementType<*, *>,
    name: StringRef?,
    val hasMessageId: Boolean,
    val messageId: Int,
) : TactNamedStub<TactMessage>(parent, elementType, name) {
    constructor(
        parent: StubElement<*>,
        elementType: IStubElementType<*, *>,
        name: String?,
        hasMessageId: Boolean,
        messageId: Int
    ) : this(
        parent,
        elementType,
        StringRef.fromString(name),
        hasMessageId,
        messageId
    )

    override fun toString(): String {
        return "TactMessageStub(name=$name, hasMessageId=$hasMessageId, messageId=$messageId)"
    }

    object Type : TactStubElementType<TactMessageStub, TactMessage>("MESSAGE") {
        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): TactMessageStub {
            return TactMessageStub(
                parentStub,
                this,
                dataStream.readName(),
                dataStream.readBoolean(),
                dataStream.readInt()
            )
        }

        override fun serialize(stub: TactMessageStub, dataStream: StubOutputStream) {
            dataStream.writeName(stub.name)
            dataStream.writeBoolean(stub.hasMessageId)
            dataStream.writeInt(stub.messageId)
        }

        override fun createPsi(stub: TactMessageStub): TactMessage {
            return TactMessageImpl(stub, this)
        }

        override fun createStub(psi: TactMessage, parentStub: StubElement<*>): TactMessageStub {
            val messageId = psi.messageId
            val messageIdValueString = messageId?.integerLiteral?.text?.replace("_", "")
            val messageIdValue = messageIdValueString?.toIntOrNull()
                ?: messageIdValueString?.toIntOrNull(16)
                ?: messageIdValueString?.toIntOrNull(2)
                ?: 0
            return TactMessageStub(parentStub, this, psi.name, messageId != null, messageIdValue)
        }

        override fun indexStub(stub: TactMessageStub, sink: IndexSink) = sink.indexMessage(stub)
    }
}
