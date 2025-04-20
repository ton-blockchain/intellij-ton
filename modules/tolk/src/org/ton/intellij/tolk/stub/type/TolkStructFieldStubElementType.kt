package org.ton.intellij.tolk.stub.type

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.ArrayFactory
import org.ton.intellij.tolk.psi.TolkStructField
import org.ton.intellij.tolk.psi.impl.TolkStructFieldImpl
import org.ton.intellij.tolk.stub.TolkStructFieldStub

class TolkStructFieldStubElementType(
    debugName: String,
) : TolkNamedStubElementType<TolkStructFieldStub, TolkStructField>(debugName) {
    override fun serialize(stub: TolkStructFieldStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): TolkStructFieldStub {
        val name = dataStream.readName()
        return TolkStructFieldStub(parentStub, this, name)
    }

    override fun createStub(
        psi: TolkStructField,
        parentStub: StubElement<out PsiElement>,
    ): TolkStructFieldStub {
        return TolkStructFieldStub(parentStub, this, psi.name)
    }

    override fun createPsi(stub: TolkStructFieldStub): TolkStructField {
        return TolkStructFieldImpl(stub, this)
    }

    companion object {
        val EMPTY_ARRAY = emptyArray<TolkStructField>()
        val ARRAY_FACTORY: ArrayFactory<TolkStructField?> = ArrayFactory {
            if (it == 0) EMPTY_ARRAY else arrayOfNulls(it)
        }
    }
}
