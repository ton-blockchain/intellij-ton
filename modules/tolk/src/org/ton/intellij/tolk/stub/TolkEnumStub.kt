package org.ton.intellij.tolk.stub

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.ArrayFactory
import com.intellij.util.io.StringRef
import org.ton.intellij.tolk.psi.TolkEnum
import org.ton.intellij.tolk.psi.impl.TolkEnumImpl
import org.ton.intellij.tolk.stub.index.TolkTypeDefIndex
import org.ton.intellij.tolk.stub.index.TolkTypeSymbolIndex
import org.ton.intellij.tolk.stub.type.TolkNamedStubElementType

class TolkEnumStub(
    parent: StubElement<*>,
    elementType: IStubElementType<*, *>,
    name: StringRef?,
    isDeprecated: Boolean,
) : TolkNamedStub<TolkEnum>(parent, elementType, name, isDeprecated) {
    object Type : TolkNamedStubElementType<TolkEnumStub, TolkEnum>("ENUM") {
        override val extraIndexKeys = listOf(TolkTypeDefIndex.KEY, TolkTypeSymbolIndex.KEY)

        override fun serialize(stub: TolkEnumStub, dataStream: StubOutputStream) {
            dataStream.writeName(stub.name)
            dataStream.writeBoolean(stub.isDeprecated)
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): TolkEnumStub {
            val name = dataStream.readName()
            val isDeprecated = dataStream.readBoolean()
            return TolkEnumStub(parentStub, this, name, isDeprecated)
        }

        override fun createStub(psi: TolkEnum, parentStub: StubElement<out PsiElement>): TolkEnumStub =
            TolkEnumStub(parentStub, this, StringRef.fromString(psi.name), psi.annotations.hasDeprecatedAnnotation())

        override fun createPsi(stub: TolkEnumStub): TolkEnum = TolkEnumImpl(stub, this)
    }

    companion object {
        val EMPTY_ARRAY = emptyArray<TolkEnum>()
        val ARRAY_FACTORY: ArrayFactory<TolkEnum> = ArrayFactory {
            if (it == 0) EMPTY_ARRAY else arrayOfNulls<TolkEnum>(it)
        }
    }
}
