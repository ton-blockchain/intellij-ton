package org.ton.intellij.tolk.stub.type

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.ArrayFactory
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.impl.*
import org.ton.intellij.tolk.stub.TolkFunctionStub
import org.ton.intellij.tolk.stub.index.TolkFunctionIndex

class TolkFunctionStubElementType(
    debugName: String,
) : TolkNamedStubElementType<TolkFunctionStub, TolkFunction>(debugName) {
    override val extraIndexKeys = listOf(TolkFunctionIndex.KEY)

    override fun serialize(stub: TolkFunctionStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        var flags = 0
        if (stub.isMutable) flags = flags or IS_MUTABLE_FLAG
        if (stub.isGetMethod) flags = flags or IS_GET_METHOD_FLAG
        if (stub.hasAsm) flags = flags or HAS_ASM
        if (stub.isBuiltin) flags = flags or IS_BUILTIN
        if (stub.isDeprecated) flags = flags or IS_DEPRECATED
        if (stub.hasSelf) flags = flags or HAS_SELF
        if (stub.hasReceiver) flags = flags or HAS_RECEIVER
        if (stub.isEntryPoint) flags = flags or IS_ENTRY_POINT
        dataStream.writeByte(flags)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): TolkFunctionStub {
        val name = dataStream.readName()?.string
        val flags = dataStream.readByte().toInt()
        val isMutable = flags and IS_MUTABLE_FLAG != 0
        val isGetMethod = flags and IS_GET_METHOD_FLAG != 0
        val hasAsm = flags and HAS_ASM != 0
        val isBuiltin = flags and IS_BUILTIN != 0
        val isDeprecated = flags and IS_DEPRECATED != 0
        val hasSelf = flags and HAS_SELF != 0
        val hasReceiver = flags and HAS_RECEIVER != 0
        val isEntryPoint = flags and IS_ENTRY_POINT != 0

        return TolkFunctionStub(
            parentStub,
            this,
            name,
            isMutable,
            isGetMethod,
            hasAsm,
            isBuiltin,
            isDeprecated,
            hasSelf,
            hasReceiver,
            isEntryPoint
        )
    }

    override fun createStub(psi: TolkFunction, parentStub: StubElement<out PsiElement>): TolkFunctionStub {
        return TolkFunctionStub(
            parentStub,
            this,
            name = psi.name,
            isMutable = psi.isMutable,
            isGetMethod = psi.isGetMethod,
            hasAsm = psi.hasAsm,
            isBuiltin = psi.isBuiltin,
            isDeprecated = psi.isDeprecated,
            hasSelf = psi.hasSelf,
            hasReceiver = psi.hasReceiver,
            isEntryPoint = psi.isEntryPoint
        )
    }

    override fun createPsi(stub: TolkFunctionStub): TolkFunction {
        return TolkFunctionImpl(stub, this)
    }

    companion object {
        private const val IS_MUTABLE_FLAG = 1 shl 0
        private const val IS_GET_METHOD_FLAG = 1 shl 1
        private const val HAS_ASM = 1 shl 2
        private const val IS_BUILTIN = 1 shl 3
        private const val IS_DEPRECATED = 1 shl 4
        private const val HAS_SELF = 1 shl 5
        private const val HAS_RECEIVER = 1 shl 6
        private const val IS_ENTRY_POINT = 1 shl 7

        val EMPTY_ARRAY = emptyArray<TolkFunction>()
        val ARRAY_FACTORY: ArrayFactory<TolkFunction?> = ArrayFactory {
            if (it == 0) EMPTY_ARRAY else arrayOfNulls(it)
        }
    }
}
