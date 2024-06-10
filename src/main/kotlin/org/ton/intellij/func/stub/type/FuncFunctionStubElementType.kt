package org.ton.intellij.func.stub.type

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.ArrayFactory
import org.ton.intellij.func.psi.FuncFunction
import org.ton.intellij.func.psi.impl.*
import org.ton.intellij.func.stub.FuncFunctionStub
import org.ton.intellij.func.stub.index.FuncNamedElementIndex

class FuncFunctionStubElementType(
    debugName: String,
) : FuncNamedStubElementType<FuncFunctionStub, FuncFunction>(debugName) {
    override fun serialize(stub: FuncFunctionStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        var flags = 0
        if (stub.isMutable) flags = flags or IS_MUTABLE_FLAG
        if (stub.isImpure) flags = flags or IS_IMPURE_FLAG
        if (stub.hasMethodId) flags = flags or HAS_METHOD_ID_FLAG
        if (stub.hasAsm) flags = flags or HAS_ASM
        if (stub.hasGet) flags = flags or HAS_GET
        if (stub.hasPure) flags = flags or HAS_PURE
        dataStream.writeByte(flags)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): FuncFunctionStub {
        val name = dataStream.readName()
        val flags = dataStream.readByte().toInt()
        val isMutable = flags and IS_MUTABLE_FLAG != 0
        val isImpure = flags and IS_IMPURE_FLAG != 0
        val hasMethodId = flags and HAS_METHOD_ID_FLAG != 0
        val hasAsm = flags and HAS_ASM != 0
        val hasGet = flags and HAS_GET != 0
        val hasPure = flags and HAS_PURE != 0
        return FuncFunctionStub(
            parentStub, this,
            name = name,
            isMutable = isMutable,
            isImpure = isImpure,
            hasMethodId = hasMethodId,
            hasAsm = hasAsm,
            hasGet = hasGet,
            hasPure = hasPure,
        )
    }

    override fun createStub(psi: FuncFunction, parentStub: StubElement<out PsiElement>): FuncFunctionStub {
        return FuncFunctionStub(
            parentStub,
            this,
            name = psi.name,
            isMutable = psi.isMutable,
            isImpure = psi.isImpure,
            hasMethodId = psi.hasMethodId,
            hasAsm = psi.hasAsm,
            hasGet = psi.hasGet,
            hasPure = psi.hasPure
        )
    }

    override fun createPsi(stub: FuncFunctionStub): FuncFunction {
        return FuncFunctionImpl(stub, this)
    }

    override fun indexStub(stub: FuncFunctionStub, sink: IndexSink) {
        val name = stub.name ?: return
        sink.occurrence(FuncNamedElementIndex.KEY, name)
    }

    companion object {
        private const val IS_MUTABLE_FLAG = 1
        private const val IS_IMPURE_FLAG = 1 shl 1
        private const val HAS_METHOD_ID_FLAG = 1 shl 2
        private const val HAS_ASM = 1 shl 3
        private const val HAS_GET = 1 shl 4
        private const val HAS_PURE = 1 shl 5

        val EMPTY_ARRAY = emptyArray<FuncFunction>()
        val ARRAY_FACTORY: ArrayFactory<FuncFunction?> = ArrayFactory {
            if (it == 0) EMPTY_ARRAY else arrayOfNulls(it)
        }
    }
}
