package org.ton.intellij.tolk.stub.type

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.ArrayFactory
import org.ton.intellij.tolk.psi.TolkConstVar
import org.ton.intellij.tolk.psi.impl.TolkConstVarImpl
import org.ton.intellij.tolk.stub.TolkConstVarStub
import org.ton.intellij.tolk.stub.index.TolkNamedElementIndex

class TolkConstVarStubElementType(
    debugName: String,
) : TolkNamedStubElementType<TolkConstVarStub, TolkConstVar>(debugName) {
    override fun serialize(stub: TolkConstVarStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): TolkConstVarStub {
        val name = dataStream.readName()
        return TolkConstVarStub(parentStub, this, name)
    }

    override fun createStub(
        psi: TolkConstVar,
        parentStub: StubElement<out PsiElement>,
    ): TolkConstVarStub {
        return TolkConstVarStub(parentStub, this, psi.name)
    }

    override fun createPsi(stub: TolkConstVarStub): TolkConstVar {
        return TolkConstVarImpl(stub, this)
    }

    companion object {
        val EMPTY_ARRAY = emptyArray<TolkConstVar>()
        val ARRAY_FACTORY: ArrayFactory<TolkConstVar?> = ArrayFactory {
            if (it == 0) EMPTY_ARRAY else arrayOfNulls(it)
        }
    }
}
