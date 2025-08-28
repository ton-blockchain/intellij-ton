package org.ton.intellij.tolk.stub

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.ArrayFactory
import com.intellij.util.io.StringRef
import org.ton.intellij.tolk.psi.TolkEnumMember
import org.ton.intellij.tolk.psi.impl.TolkEnumMemberImpl
import org.ton.intellij.tolk.stub.type.TolkNamedStubElementType

class TolkEnumMemberStub(
    parent: StubElement<*>,
    elementType: IStubElementType<*, *>,
    name: StringRef?,
) : TolkNamedStub<TolkEnumMember>(parent, elementType, name, false) {
    constructor(
        parent: StubElement<*>, elementType: IStubElementType<*, *>,
        name: String?,
    ) : this(
        parent,
        elementType,
        StringRef.fromString(name),
    )

    class Type(
        debugName: String,
    ) : TolkNamedStubElementType<TolkEnumMemberStub, TolkEnumMember>(debugName) {
        override fun serialize(stub: TolkEnumMemberStub, dataStream: StubOutputStream) {
            dataStream.writeName(stub.name)
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): TolkEnumMemberStub {
            val name = dataStream.readName()
            return TolkEnumMemberStub(parentStub, this, name)
        }

        override fun createStub(
            psi: TolkEnumMember,
            parentStub: StubElement<out PsiElement>,
        ): TolkEnumMemberStub {
            return TolkEnumMemberStub(parentStub, this, psi.name)
        }

        override fun createPsi(stub: TolkEnumMemberStub): TolkEnumMember {
            return TolkEnumMemberImpl(stub, this)
        }

        companion object {
            val EMPTY_ARRAY = emptyArray<TolkEnumMember>()
            val ARRAY_FACTORY: ArrayFactory<TolkEnumMember?> = ArrayFactory {
                if (it == 0) EMPTY_ARRAY else arrayOfNulls(it)
            }
        }
    }

}
