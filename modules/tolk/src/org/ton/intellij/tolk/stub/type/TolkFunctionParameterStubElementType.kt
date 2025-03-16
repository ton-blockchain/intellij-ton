package org.ton.intellij.tolk.stub.type

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.ArrayFactory
import org.ton.intellij.tolk.psi.TolkParameter
import org.ton.intellij.tolk.psi.impl.TolkParameterImpl
import org.ton.intellij.tolk.stub.TolkParameterStub

class TolkParameterStubElementType(
    debugName: String,
) : TolkNamedStubElementType<TolkParameterStub, TolkParameter>(debugName) {
    override fun serialize(stub: TolkParameterStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeBoolean(stub.isMutable)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): TolkParameterStub {
        val name = dataStream.readName()
        val mutable = dataStream.readBoolean()
        return TolkParameterStub(parentStub, this, name, mutable)
    }

    override fun createStub(
        psi: TolkParameter,
        parentStub: StubElement<out PsiElement>,
    ): TolkParameterStub {
        return TolkParameterStub(parentStub, this, psi.name, psi.mutateKeyword != null)
    }

    override fun createPsi(stub: TolkParameterStub): TolkParameter {
        return TolkParameterImpl(stub, this)
    }

    companion object {
        val EMPTY_ARRAY = emptyArray<TolkParameter>()
        val ARRAY_FACTORY: ArrayFactory<TolkParameter?> = ArrayFactory {
            if (it == 0) EMPTY_ARRAY else arrayOfNulls(it)
        }
    }
}
