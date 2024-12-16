package org.ton.intellij.tolk.stub.type

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.ArrayFactory
import org.ton.intellij.tolk.psi.TolkTypeParameter
import org.ton.intellij.tolk.psi.impl.TolkTypeParameterImpl
import org.ton.intellij.tolk.stub.TolkTypeParameterStub

class TolkTypeParameterStubElementType(
    debugName: String,
) : TolkNamedStubElementType<TolkTypeParameterStub, TolkTypeParameter>(debugName) {
    override fun serialize(stub: TolkTypeParameterStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): TolkTypeParameterStub {
        val name = dataStream.readName()
        return TolkTypeParameterStub(parentStub, this, name)
    }

    override fun createStub(
        psi: TolkTypeParameter,
        parentStub: StubElement<out PsiElement>,
    ): TolkTypeParameterStub {
        return TolkTypeParameterStub(parentStub, this, psi.name)
    }

    override fun createPsi(stub: TolkTypeParameterStub): TolkTypeParameter {
        return TolkTypeParameterImpl(stub, this)
    }

    companion object {
        val EMPTY_ARRAY = emptyArray<TolkTypeParameter>()
        val ARRAY_FACTORY: ArrayFactory<TolkTypeParameter?> = ArrayFactory {
            if (it == 0) EMPTY_ARRAY else arrayOfNulls(it)
        }
    }
}
