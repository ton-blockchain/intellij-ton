package org.ton.intellij.tolk.stub.type

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.ArrayFactory
import org.ton.intellij.tolk.psi.TolkTypeDef
import org.ton.intellij.tolk.psi.impl.TolkTypeDefImpl
import org.ton.intellij.tolk.stub.TolkTypeDefStub
import org.ton.intellij.tolk.stub.index.TolkTypeDefIndex

class TolkTypeDefStubElementType(
    debugName: String,
) : TolkNamedStubElementType<TolkTypeDefStub, TolkTypeDef>(debugName) {
    override val extraIndexKeys = listOf(TolkTypeDefIndex.KEY)

    override fun serialize(stub: TolkTypeDefStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): TolkTypeDefStub {
        val name = dataStream.readName()
        return TolkTypeDefStub(parentStub, this, name)
    }

    override fun createStub(
        psi: TolkTypeDef,
        parentStub: StubElement<out PsiElement>,
    ): TolkTypeDefStub {
        return TolkTypeDefStub(parentStub, this, psi.name)
    }

    override fun createPsi(stub: TolkTypeDefStub): TolkTypeDef {
        return TolkTypeDefImpl(stub, this)
    }

    companion object {
        val EMPTY_ARRAY = emptyArray<TolkTypeDef>()
        val ARRAY_FACTORY: ArrayFactory<TolkTypeDef?> = ArrayFactory {
            if (it == 0) EMPTY_ARRAY else arrayOfNulls(it)
        }
    }
}
