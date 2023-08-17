package org.ton.intellij.func.stub.type

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.ArrayFactory
import org.ton.intellij.func.psi.FuncConstVar
import org.ton.intellij.func.psi.impl.FuncConstVarImpl
import org.ton.intellij.func.stub.FuncConstVarStub

class FuncConstVarStubElementType(
    debugName: String,
) : FuncNamedStubElementType<FuncConstVarStub, FuncConstVar>(debugName) {
    override fun serialize(stub: FuncConstVarStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): FuncConstVarStub {
        val name = dataStream.readName()
        return FuncConstVarStub(parentStub, this, name)
    }

    override fun createStub(
        psi: FuncConstVar,
        parentStub: StubElement<out PsiElement>,
    ): FuncConstVarStub {
        return FuncConstVarStub(parentStub, this, psi.name)
    }

    override fun createPsi(stub: FuncConstVarStub): FuncConstVar {
        return FuncConstVarImpl(stub, this)
    }

    companion object {
        val EMPTY_ARRAY = emptyArray<FuncConstVar>()
        val ARRAY_FACTORY: ArrayFactory<FuncConstVar?> = ArrayFactory {
            if (it == 0) EMPTY_ARRAY else arrayOfNulls(it)
        }
    }
}
