package org.ton.intellij.func.stub.type

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.ArrayFactory
import org.ton.intellij.func.psi.FuncFunctionParameter
import org.ton.intellij.func.psi.impl.FuncFunctionParameterImpl
import org.ton.intellij.func.stub.FuncFunctionParameterStub

class FuncFunctionParameterStubElementType(
    debugName: String,
) : FuncNamedStubElementType<FuncFunctionParameterStub, FuncFunctionParameter>(debugName) {
    override fun serialize(stub: FuncFunctionParameterStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): FuncFunctionParameterStub {
        val name = dataStream.readName()
        return FuncFunctionParameterStub(parentStub, this, name)
    }

    override fun createStub(
        psi: FuncFunctionParameter,
        parentStub: StubElement<out PsiElement>,
    ): FuncFunctionParameterStub {
        return FuncFunctionParameterStub(parentStub, this, psi.name)
    }

    override fun createPsi(stub: FuncFunctionParameterStub): FuncFunctionParameter {
        return FuncFunctionParameterImpl(stub, this)
    }

    companion object {
        val EMPTY_ARRAY = emptyArray<FuncFunctionParameter>()
        val ARRAY_FACTORY: ArrayFactory<FuncFunctionParameter?> = ArrayFactory {
            if (it == 0) EMPTY_ARRAY else arrayOfNulls(it)
        }
    }
}
