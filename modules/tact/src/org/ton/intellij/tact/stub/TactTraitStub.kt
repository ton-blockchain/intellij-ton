package org.ton.intellij.tact.stub

import com.intellij.psi.stubs.*
import com.intellij.util.ArrayFactory
import com.intellij.util.io.StringRef
import org.ton.intellij.tact.psi.TactTrait
import org.ton.intellij.tact.psi.impl.TactTraitImpl
import org.ton.intellij.tact.stub.index.indexTrait

class TactTraitStub(
    parent: StubElement<*>,
    elementType: IStubElementType<*, *>,
    name: StringRef?,
    val withClause: List<StringRef?>,
) : TactNamedStub<TactTrait>(parent, elementType, name) {
    constructor(
        parent: StubElement<*>,
        elementType: IStubElementType<*, *>,
        name: String?,
        withClause: List<String>
    ) : this(
        parent,
        elementType,
        StringRef.fromString(name),
        withClause.map { StringRef.fromString(it) }
    )

    object Type : TactStubElementType<TactTraitStub, TactTrait>("TRAIT") {
        val EMPTY_ARRAY = emptyArray<TactTrait>()
        val ARRAY_FACTORY: ArrayFactory<TactTrait?> = ArrayFactory {
            if (it == 0) EMPTY_ARRAY else arrayOfNulls(it)
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): TactTraitStub {
            return TactTraitStub(
                parentStub,
                this,
                dataStream.readName(),
                List(dataStream.readVarInt()) { dataStream.readName() })
        }

        override fun serialize(stub: TactTraitStub, dataStream: StubOutputStream) {
            dataStream.writeName(stub.name)
            dataStream.writeVarInt(stub.withClause.size)
            stub.withClause.forEach { dataStream.writeName(it?.string) }
        }

        override fun createPsi(stub: TactTraitStub): TactTrait {
            return TactTraitImpl(stub, this)
        }

        override fun createStub(psi: TactTrait, parentStub: StubElement<*>): TactTraitStub {
            return TactTraitStub(parentStub, this, psi.name, psi.withClause?.typeList?.map { it.text } ?: emptyList())
        }

        override fun indexStub(stub: TactTraitStub, sink: IndexSink) = sink.indexTrait(stub)
    }
}
