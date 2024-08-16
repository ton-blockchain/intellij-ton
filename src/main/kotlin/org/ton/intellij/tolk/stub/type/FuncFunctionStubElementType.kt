package org.ton.intellij.tolk.stub.type

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.ArrayFactory
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.impl.*
import org.ton.intellij.tolk.stub.TolkFunctionStub
import org.ton.intellij.tolk.stub.index.TolkNamedElementIndex

class TolkFunctionStubElementType(
    debugName: String,
) : TolkNamedStubElementType<TolkFunctionStub, TolkFunction>(debugName) {
    override fun serialize(stub: TolkFunctionStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        var flags = 0
        if (stub.isMutable) flags = flags or IS_MUTABLE_FLAG
        if (stub.isImpure) flags = flags or IS_IMPURE_FLAG
        if (stub.hasMethodId) flags = flags or HAS_METHOD_ID_FLAG
        if (stub.hasAsm) flags = flags or HAS_ASM
        dataStream.writeByte(flags)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): TolkFunctionStub {
        val name = dataStream.readName()
        val flags = dataStream.readByte().toInt()
        val isMutable = flags and IS_MUTABLE_FLAG != 0
        val isImpure = flags and IS_IMPURE_FLAG != 0
        val hasMethodId = flags and HAS_METHOD_ID_FLAG != 0
        val hasAsm = flags and HAS_ASM != 0
        return TolkFunctionStub(parentStub, this, name, isMutable, isImpure, hasMethodId, hasAsm)
    }

    override fun createStub(psi: TolkFunction, parentStub: StubElement<out PsiElement>): TolkFunctionStub {
        return TolkFunctionStub(parentStub, this, psi.name, psi.isMutable, psi.isImpure, psi.hasMethodId, psi.hasAsm)
    }

    override fun createPsi(stub: TolkFunctionStub): TolkFunction {
        return TolkFunctionImpl(stub, this)
    }

    override fun indexStub(stub: TolkFunctionStub, sink: IndexSink) {
        val name = stub.name ?: return
        sink.occurrence(TolkNamedElementIndex.KEY, name)
    }

    companion object {
        private const val IS_MUTABLE_FLAG = 0x1
        private const val IS_IMPURE_FLAG = 0x2
        private const val HAS_METHOD_ID_FLAG = 0x4
        private const val HAS_ASM = 0x8

        val EMPTY_ARRAY = emptyArray<TolkFunction>()
        val ARRAY_FACTORY: ArrayFactory<TolkFunction?> = ArrayFactory {
            if (it == 0) EMPTY_ARRAY else arrayOfNulls(it)
        }
    }
}
