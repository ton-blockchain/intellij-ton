package org.ton.intellij.tolk.stub.type

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.ArrayFactory
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.impl.*
import org.ton.intellij.tolk.stub.TolkFunctionStub

class TolkFunctionStubElementType(
    debugName: String,
) : TolkNamedStubElementType<TolkFunctionStub, TolkFunction>(debugName) {
    override fun serialize(stub: TolkFunctionStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        var flags = 0
        if (stub.isMutable) flags = flags or IS_MUTABLE_FLAG
        if (stub.isGetMethod) flags = flags or IS_GET_METHOD_FLAG
        if (stub.hasAsm) flags = flags or HAS_ASM
        if (stub.isBuiltin) flags = flags or IS_BUILTIN
        if (stub.isDeprecated) flags = flags or IS_DEPRECATED
        if (stub.isGeneric) flags = flags or IS_GENERIC
        dataStream.writeByte(flags)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): TolkFunctionStub {
        val name = dataStream.readName()
        val flags = dataStream.readByte().toInt()
        val isMutable = flags and IS_MUTABLE_FLAG != 0
        val isGetMethod = flags and IS_GET_METHOD_FLAG != 0
        val hasAsm = flags and HAS_ASM != 0
        val isBuiltin = flags and IS_BUILTIN != 0
        val isDeprecated = flags and IS_DEPRECATED != 0
        val isGenerics = flags and IS_GENERIC != 0
        return TolkFunctionStub(parentStub, this, name, isMutable, isGetMethod, hasAsm, isBuiltin, isDeprecated, isGenerics)
    }

    override fun createStub(psi: TolkFunction, parentStub: StubElement<out PsiElement>): TolkFunctionStub {
        return TolkFunctionStub(
            parentStub,
            this,
            psi.name,
            psi.isMutable,
            psi.isGetMethod,
            psi.hasAsm,
            psi.isBuiltin,
            psi.isDeprecated,
            psi.isGeneric
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
        private const val IS_GENERIC = 1 shl 5

        val EMPTY_ARRAY = emptyArray<TolkFunction>()
        val ARRAY_FACTORY: ArrayFactory<TolkFunction?> = ArrayFactory {
            if (it == 0) EMPTY_ARRAY else arrayOfNulls(it)
        }
    }
}
