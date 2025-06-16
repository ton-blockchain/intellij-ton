package org.ton.intellij.tolk.stub

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.ArrayFactory
import com.intellij.util.io.StringRef
import org.ton.intellij.tolk.psi.TolkTypeDef
import org.ton.intellij.tolk.psi.impl.TolkTypeDefImpl
import org.ton.intellij.tolk.stub.index.TolkTypeDefIndex
import org.ton.intellij.tolk.stub.index.TolkTypeSymbolIndex
import org.ton.intellij.tolk.stub.type.TolkNamedStubElementType

class TolkTypeDefStub(
    parent: StubElement<*>,
    elementType: IStubElementType<*, *>,
    name: StringRef?,
    val isDeprecated: Boolean
) : TolkNamedStub<TolkTypeDef>(parent, elementType, name) {
    object Type : TolkNamedStubElementType<TolkTypeDefStub, TolkTypeDef>("TYPE_DEF") {
        override val extraIndexKeys = listOf(TolkTypeDefIndex.KEY, TolkTypeSymbolIndex.KEY)

        override fun serialize(stub: TolkTypeDefStub, dataStream: StubOutputStream) {
            dataStream.writeName(stub.name)
            dataStream.writeBoolean(stub.isDeprecated)
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): TolkTypeDefStub {
            val name = dataStream.readName()
            val isDeprecated = dataStream.readBoolean()
            return TolkTypeDefStub(parentStub, this, name, isDeprecated)
        }

        override fun createStub(psi: TolkTypeDef, parentStub: StubElement<out PsiElement>): TolkTypeDefStub =
            TolkTypeDefStub(parentStub, this, StringRef.fromString(psi.name), psi.isDeprecated)

        override fun createPsi(stub: TolkTypeDefStub): TolkTypeDef =
            TolkTypeDefImpl(stub, this)
    }

    companion object {
        val EMPTY_ARRAY = emptyArray<TolkTypeDef>()
        val ARRAY_FACTORY: ArrayFactory<TolkTypeDef?> = ArrayFactory {
            if (it == 0) EMPTY_ARRAY else arrayOfNulls(it)
        }
    }
}
