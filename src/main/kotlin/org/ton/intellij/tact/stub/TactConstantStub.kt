package org.ton.intellij.tact.stub

import com.intellij.psi.stubs.*
import com.intellij.util.ArrayFactory
import com.intellij.util.BitUtil
import com.intellij.util.io.StringRef
import org.ton.intellij.tact.psi.TactConstant
import org.ton.intellij.tact.psi.impl.TactConstantImpl
import org.ton.intellij.tact.stub.index.indexConstant
import org.ton.intellij.util.BitFlagsBuilder

class TactConstantStub(
    parent: StubElement<*>,
    elementType: IStubElementType<*, *>,
    name: StringRef?,
    val flags: Byte,
) : TactNamedStub<TactConstant>(parent, elementType, name) {
    constructor(parent: StubElement<*>, elementType: IStubElementType<*, *>, name: String?, flags: Byte) : this(
        parent,
        elementType,
        StringRef.fromString(name),
        flags
    )

    val isAbstract get() = BitUtil.isSet(flags, Flags.ABSTRACT)
    val isOverride get() = BitUtil.isSet(flags, Flags.OVERRIDE)
    val isVirtual get() = BitUtil.isSet(flags, Flags.VIRTUAL)

    override fun toString(): String {
        return "${javaClass.simpleName}(name=$name, flags=$flags, isAbstract=$isAbstract, isOverride=$isOverride, isVirtual=$isVirtual)"
    }

    object Type : TactStubElementType<TactConstantStub, TactConstant>("CONSTANT") {
        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): TactConstantStub {
            return TactConstantStub(parentStub, this, dataStream.readName(), dataStream.readByte())
        }

        override fun serialize(stub: TactConstantStub, dataStream: StubOutputStream) {
            dataStream.writeName(stub.name)
            dataStream.writeByte(stub.flags.toInt())
        }

        override fun createPsi(stub: TactConstantStub): TactConstant {
            return TactConstantImpl(stub, this)
        }

        override fun createStub(psi: TactConstant, parentStub: StubElement<*>): TactConstantStub {
            var flags = 0.toByte()
            psi.constantAttributeList.forEach {
                if (it.virtualKeyword != null) {
                    flags = BitUtil.set(flags, Flags.VIRTUAL, true)
                }
                if (it.overrideKeyword != null) {
                    flags = BitUtil.set(flags, Flags.OVERRIDE, true)
                }
                if (it.abstractKeyword != null) {
                    flags = BitUtil.set(flags, Flags.ABSTRACT, true)
                }
            }
            return TactConstantStub(parentStub, this, psi.name, flags)
        }

        override fun indexStub(stub: TactConstantStub, sink: IndexSink) = sink.indexConstant(stub)

        val EMPTY_ARRAY = emptyArray<TactConstant>()
        val ARRAY_FACTORY: ArrayFactory<TactConstant?> = ArrayFactory {
            if (it == 0) EMPTY_ARRAY else arrayOfNulls(it)
        }
    }

    private object Flags : BitFlagsBuilder(Limit.BYTE) {
        val ABSTRACT = nextBitMask().toByte()
        val OVERRIDE = nextBitMask().toByte()
        val VIRTUAL = nextBitMask().toByte()
    }
}
