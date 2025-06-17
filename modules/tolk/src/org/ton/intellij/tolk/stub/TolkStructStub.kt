package org.ton.intellij.tolk.stub

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.ArrayFactory
import com.intellij.util.io.StringRef
import org.ton.intellij.tolk.psi.TolkStruct
import org.ton.intellij.tolk.psi.impl.TolkStructImpl
import org.ton.intellij.tolk.stub.index.TolkStructIndex
import org.ton.intellij.tolk.stub.index.TolkTypeSymbolIndex
import org.ton.intellij.tolk.stub.type.TolkNamedStubElementType

class TolkStructStub(
    parent: StubElement<*>,
    elementType: IStubElementType<*, *>,
    name: StringRef?,
    val isDeprecated: Boolean,
) : TolkNamedStub<TolkStruct>(parent, elementType, name) {
    object Type : TolkNamedStubElementType<TolkStructStub, TolkStruct>("STRUCT") {
        override val extraIndexKeys = listOf(TolkStructIndex.KEY, TolkTypeSymbolIndex.KEY)

        override fun serialize(stub: TolkStructStub, dataStream: StubOutputStream) {
            dataStream.writeName(stub.name)
            dataStream.writeBoolean(stub.isDeprecated)
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): TolkStructStub {
            val name = dataStream.readName()
            val isDeprecated = dataStream.readBoolean()
            return TolkStructStub(parentStub, this, name, isDeprecated)
        }

        override fun createStub(psi: TolkStruct, parentStub: StubElement<out PsiElement>): TolkStructStub =
            TolkStructStub(parentStub, this, StringRef.fromString(psi.name), psi.annotations.hasDeprecatedAnnotation())

        override fun createPsi(stub: TolkStructStub): TolkStruct = TolkStructImpl(stub, this)
    }

    companion object {
        val EMPTY_ARRAY = emptyArray<TolkStruct>()
        val ARRAY_FACTORY: ArrayFactory<TolkStruct?> = ArrayFactory {
            if (it == 0) EMPTY_ARRAY else arrayOfNulls(it)
        }
    }
}
