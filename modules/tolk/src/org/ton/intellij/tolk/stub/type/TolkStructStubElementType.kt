package org.ton.intellij.tolk.stub.type

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.ArrayFactory
import org.ton.intellij.tolk.psi.TolkStruct
import org.ton.intellij.tolk.psi.impl.TolkStructImpl
import org.ton.intellij.tolk.stub.TolkStructStub

class TolkStructStubElementType(
    debugName: String,
) : TolkNamedStubElementType<TolkStructStub, TolkStruct>(debugName) {
    override fun serialize(stub: TolkStructStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): TolkStructStub {
        val name = dataStream.readName()
        return TolkStructStub(parentStub, this, name)
    }

    override fun createStub(
        psi: TolkStruct,
        parentStub: StubElement<out PsiElement>,
    ): TolkStructStub {
        return TolkStructStub(parentStub, this, psi.name)
    }

    override fun createPsi(stub: TolkStructStub): TolkStruct {
        return TolkStructImpl(stub, this)
    }

    companion object {
        val EMPTY_ARRAY = emptyArray<TolkStruct>()
        val ARRAY_FACTORY: ArrayFactory<TolkStruct?> = ArrayFactory {
            if (it == 0) EMPTY_ARRAY else arrayOfNulls(it)
        }
    }
}
