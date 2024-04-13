package org.ton.intellij.tact.stub

import com.intellij.psi.stubs.*
import com.intellij.util.BitUtil
import com.intellij.util.io.StringRef
import org.ton.intellij.tact.psi.TactFunction
import org.ton.intellij.tact.psi.impl.*
import org.ton.intellij.tact.stub.index.indexFunction
import org.ton.intellij.util.BitFlagsBuilder

class TactFunctionStub(
    parent: StubElement<*>,
    elementType: IStubElementType<*, *>,
    name: StringRef?,
    val flags: Int,
) : TactNamedStub<TactFunction>(parent, elementType, name) {
    constructor(
        parent: StubElement<*>, elementType: IStubElementType<*, *>,
        name: String?,
        flags: Int,
    ) : this(
        parent,
        elementType,
        StringRef.fromString(name),
        flags
    )

    val isNative get() = BitUtil.isSet(flags, Flags.NATIVE)
    val isGet get() = BitUtil.isSet(flags, Flags.GET)
    val isMutates get() = BitUtil.isSet(flags, Flags.MUTATES)
    val isExtends get() = BitUtil.isSet(flags, Flags.EXTENDS)
    val isVirtual get() = BitUtil.isSet(flags, Flags.VIRTUAL)
    val isOverride get() = BitUtil.isSet(flags, Flags.OVERRIDE)
    val isInline get() = BitUtil.isSet(flags, Flags.INLINE)
    val isAbstract get() = BitUtil.isSet(flags, Flags.ABSTRACT)


    object Type : TactStubElementType<TactFunctionStub, TactFunction>("FUNCTION") {
        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): TactFunctionStub {
            return TactFunctionStub(parentStub, this, dataStream.readName(), dataStream.readInt())
        }

        override fun serialize(stub: TactFunctionStub, dataStream: StubOutputStream) {
            dataStream.writeName(stub.name)
            dataStream.writeInt(stub.flags)
        }

        override fun createPsi(stub: TactFunctionStub): TactFunction {
            return TactFunctionImpl(stub, this)
        }

        override fun createStub(psi: TactFunction, parentStub: StubElement<*>): TactFunctionStub {
            var flags = 0
            flags = BitUtil.set(flags, Flags.NATIVE, psi.isNative)
            flags = BitUtil.set(flags, Flags.GET, psi.isGet)
            flags = BitUtil.set(flags, Flags.MUTATES, psi.isMutates)
            flags = BitUtil.set(flags, Flags.EXTENDS, psi.isExtends)
            flags = BitUtil.set(flags, Flags.VIRTUAL, psi.isVirtual)
            flags = BitUtil.set(flags, Flags.OVERRIDE, psi.isOverride)
            flags = BitUtil.set(flags, Flags.INLINE, psi.isInline)
            flags = BitUtil.set(flags, Flags.ABSTRACT, psi.isAbstract)
            return TactFunctionStub(parentStub, this, psi.name, flags)
        }

        override fun indexStub(stub: TactFunctionStub, sink: IndexSink) = sink.indexFunction(stub)
    }

    private object Flags : BitFlagsBuilder(Limit.INT) {
        val NATIVE = nextBitMask()
        val GET = nextBitMask()
        val MUTATES = nextBitMask()
        val EXTENDS = nextBitMask()
        val VIRTUAL = nextBitMask()
        val OVERRIDE = nextBitMask()
        val INLINE = nextBitMask()
        val ABSTRACT = nextBitMask()
    }

    override fun toString(): String {
        return "TactFunctionStub(name=${name}, flags=$flags, isNative=$isNative, isGet=$isGet, isMutates=$isMutates, isExtends=$isExtends, isVirtual=$isVirtual, isOverride=$isOverride, isInline=$isInline, isAbstract=$isAbstract)"
    }
}
