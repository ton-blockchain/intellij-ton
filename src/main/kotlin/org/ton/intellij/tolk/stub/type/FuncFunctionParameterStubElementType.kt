package org.ton.intellij.tolk.stub.type

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.ArrayFactory
import org.ton.intellij.tolk.psi.TolkFunctionParameter
import org.ton.intellij.tolk.psi.impl.TolkFunctionParameterImpl
import org.ton.intellij.tolk.stub.TolkFunctionParameterStub

class TolkFunctionParameterStubElementType(
    debugName: String,
) : TolkNamedStubElementType<TolkFunctionParameterStub, TolkFunctionParameter>(debugName) {
    override fun serialize(stub: TolkFunctionParameterStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): TolkFunctionParameterStub {
        val name = dataStream.readName()
        return TolkFunctionParameterStub(parentStub, this, name)
    }

    override fun createStub(
        psi: TolkFunctionParameter,
        parentStub: StubElement<out PsiElement>,
    ): TolkFunctionParameterStub {
        return TolkFunctionParameterStub(parentStub, this, psi.name)
    }

    override fun createPsi(stub: TolkFunctionParameterStub): TolkFunctionParameter {
        return TolkFunctionParameterImpl(stub, this)
    }

    companion object {
        val EMPTY_ARRAY = emptyArray<TolkFunctionParameter>()
        val ARRAY_FACTORY: ArrayFactory<TolkFunctionParameter?> = ArrayFactory {
            if (it == 0) EMPTY_ARRAY else arrayOfNulls(it)
        }
    }
}
