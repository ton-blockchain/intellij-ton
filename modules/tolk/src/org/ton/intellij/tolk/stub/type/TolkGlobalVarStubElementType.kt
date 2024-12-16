package org.ton.intellij.tolk.stub.type

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.ArrayFactory
import org.ton.intellij.tolk.psi.TolkGlobalVar
import org.ton.intellij.tolk.psi.impl.TolkGlobalVarImpl
import org.ton.intellij.tolk.stub.TolkGlobalVarStub
import org.ton.intellij.tolk.stub.index.TolkNamedElementIndex

class TolkGlobalVarStubElementType(
    debugName: String,
) : TolkNamedStubElementType<TolkGlobalVarStub, TolkGlobalVar>(debugName) {
    override fun serialize(stub: TolkGlobalVarStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): TolkGlobalVarStub {
        val name = dataStream.readName()
        return TolkGlobalVarStub(parentStub, this, name)
    }

    override fun createStub(
        psi: TolkGlobalVar,
        parentStub: StubElement<out PsiElement>,
    ): TolkGlobalVarStub {
        return TolkGlobalVarStub(parentStub, this, psi.name)
    }

    override fun createPsi(stub: TolkGlobalVarStub): TolkGlobalVar {
        return TolkGlobalVarImpl(stub, this)
    }

    companion object {
        val EMPTY_ARRAY = emptyArray<TolkGlobalVar>()
        val ARRAY_FACTORY: ArrayFactory<TolkGlobalVar?> = ArrayFactory {
            if (it == 0) EMPTY_ARRAY else arrayOfNulls(it)
        }
    }
}
