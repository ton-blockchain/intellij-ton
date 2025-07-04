package org.ton.intellij.tolk.stub.type

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.ArrayFactory
import org.ton.intellij.tolk.psi.TolkIncludeDefinition
import org.ton.intellij.tolk.psi.impl.TolkIncludeDefinitionImpl
import org.ton.intellij.tolk.psi.impl.path
import org.ton.intellij.tolk.stub.TolkIncludeDefinitionStub

class TolkIncludeDefinitionStubElementType(
    debugName: String,
) : TolkStubElementType<TolkIncludeDefinitionStub, TolkIncludeDefinition>(
    debugName
) {
    override fun serialize(stub: TolkIncludeDefinitionStub, dataStream: StubOutputStream) {
        dataStream.writeUTFFast(stub.path)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): TolkIncludeDefinitionStub {
        val path = dataStream.readUTFFast()
        return TolkIncludeDefinitionStub(parentStub, this, path)
    }

    override fun createStub(
        psi: TolkIncludeDefinition,
        parentStub: StubElement<out PsiElement>,
    ): TolkIncludeDefinitionStub {
        return TolkIncludeDefinitionStub(parentStub, this, psi.path)
    }

    override fun createPsi(stub: TolkIncludeDefinitionStub): TolkIncludeDefinition {
        return TolkIncludeDefinitionImpl(stub, this)
    }

    companion object {
        val EMPTY_ARRAY = emptyArray<TolkIncludeDefinition>()
        val ARRAY_FACTORY: ArrayFactory<TolkIncludeDefinition> = ArrayFactory {
            if (it == 0) EMPTY_ARRAY else arrayOfNulls<TolkIncludeDefinition>(it)
        }
    }
}
