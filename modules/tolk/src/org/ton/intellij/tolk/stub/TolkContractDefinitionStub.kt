package org.ton.intellij.tolk.stub

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.ArrayFactory
import com.intellij.util.io.StringRef
import org.ton.intellij.tolk.psi.TolkContractDefinition
import org.ton.intellij.tolk.psi.impl.TolkContractDefinitionImpl
import org.ton.intellij.tolk.stub.index.TolkNamedElementIndex
import org.ton.intellij.tolk.stub.type.TolkNamedStubElementType

class TolkContractDefinitionStub(
    parent: StubElement<*>,
    elementType: IStubElementType<*, *>,
    name: StringRef?,
    isDeprecated: Boolean,
) : TolkNamedStub<TolkContractDefinition>(parent, elementType, name, isDeprecated) {
    object Type : TolkNamedStubElementType<TolkContractDefinitionStub, TolkContractDefinition>("CONTRACT_DEFINITION") {
        override val extraIndexKeys = listOf(TolkNamedElementIndex.KEY)

        override fun serialize(stub: TolkContractDefinitionStub, dataStream: StubOutputStream) {
            dataStream.writeName(stub.name)
            dataStream.writeBoolean(stub.isDeprecated)
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): TolkContractDefinitionStub {
            val name = dataStream.readName()
            val isDeprecated = dataStream.readBoolean()
            return TolkContractDefinitionStub(parentStub, this, name, isDeprecated)
        }

        override fun createStub(psi: TolkContractDefinition, parentStub: StubElement<out PsiElement>): TolkContractDefinitionStub =
            TolkContractDefinitionStub(parentStub, this, StringRef.fromString(psi.name), false)

        override fun createPsi(stub: TolkContractDefinitionStub): TolkContractDefinition = TolkContractDefinitionImpl(stub, this)
    }

    companion object {
        val EMPTY_ARRAY = emptyArray<TolkContractDefinition>()
        val ARRAY_FACTORY: ArrayFactory<TolkContractDefinition> = ArrayFactory {
            if (it == 0) EMPTY_ARRAY else arrayOfNulls<TolkContractDefinition>(it)
        }
    }
}
