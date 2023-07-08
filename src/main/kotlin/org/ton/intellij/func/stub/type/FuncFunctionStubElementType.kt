package org.ton.intellij.func.stub.type

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.ArrayFactory
import org.ton.intellij.func.psi.FuncFunction
import org.ton.intellij.func.psi.impl.FuncFunctionImpl
import org.ton.intellij.func.stub.FuncFunctionStub

class FuncFunctionStubElementType(
    debugName: String,
) : FuncNamedStubElementType<FuncFunctionStub, FuncFunction>(debugName) {
    override fun serialize(stub: FuncFunctionStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        var flags = 0
        if (stub.isMutable) flags = flags or IS_MUTABLE_FLAG
        if (stub.isImpure) flags = flags or IS_IMPURE_FLAG
        if (stub.hasMethodId) flags = flags or HAS_METHOD_ID_FLAG
        dataStream.writeByte(flags)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): FuncFunctionStub {
        val name = dataStream.readName()
        val flags = dataStream.readByte().toInt()
        val isMutable = flags and IS_MUTABLE_FLAG != 0
        val isImpure = flags and IS_IMPURE_FLAG != 0
        val hasMethodId = flags and HAS_METHOD_ID_FLAG != 0
        return FuncFunctionStub(parentStub, this, name, isMutable, isImpure, hasMethodId)
    }

    override fun createStub(psi: FuncFunction, parentStub: StubElement<out PsiElement>): FuncFunctionStub {
        return FuncFunctionStub(parentStub, this, psi.name, psi.isMutable, psi.isImpure, psi.hasMethodId)
    }

    override fun createPsi(stub: FuncFunctionStub): FuncFunction {
        return FuncFunctionImpl(stub, this)
    }

    companion object {
        private const val IS_MUTABLE_FLAG = 0x1
        private const val IS_IMPURE_FLAG = 0x2
        private const val HAS_METHOD_ID_FLAG = 0x4

        val EMPTY_ARRAY = emptyArray<FuncFunction>()
        val ARRAY_FACTORY: ArrayFactory<FuncFunction?> = ArrayFactory {
            if (it == 0) EMPTY_ARRAY else arrayOfNulls(it)
        }
    }
}
