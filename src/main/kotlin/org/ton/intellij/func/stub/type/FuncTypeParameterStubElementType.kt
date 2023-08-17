package org.ton.intellij.func.stub.type

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.ArrayFactory
import org.ton.intellij.func.psi.FuncTypeParameter
import org.ton.intellij.func.psi.impl.FuncTypeParameterImpl
import org.ton.intellij.func.stub.FuncTypeParameterStub

class FuncTypeParameterStubElementType(
    debugName: String,
) : FuncNamedStubElementType<FuncTypeParameterStub, FuncTypeParameter>(debugName) {
    override fun serialize(stub: FuncTypeParameterStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): FuncTypeParameterStub {
        val name = dataStream.readName()
        return FuncTypeParameterStub(parentStub, this, name)
    }

    override fun createStub(
        psi: FuncTypeParameter,
        parentStub: StubElement<out PsiElement>,
    ): FuncTypeParameterStub {
        return FuncTypeParameterStub(parentStub, this, psi.name)
    }

    override fun createPsi(stub: FuncTypeParameterStub): FuncTypeParameter {
        return FuncTypeParameterImpl(stub, this)
    }

    companion object {
        val EMPTY_ARRAY = emptyArray<FuncTypeParameter>()
        val ARRAY_FACTORY: ArrayFactory<FuncTypeParameter?> = ArrayFactory {
            if (it == 0) EMPTY_ARRAY else arrayOfNulls(it)
        }
    }
}
