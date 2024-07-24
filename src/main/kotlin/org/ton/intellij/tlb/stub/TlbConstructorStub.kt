package org.ton.intellij.tlb.stub

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import com.intellij.util.ArrayFactory
import com.intellij.util.io.StringRef
import org.ton.intellij.tlb.psi.TlbConstructor
import org.ton.intellij.tlb.psi.impl.TlbConstructorImpl
import org.ton.intellij.tlb.stub.index.indexTlbConstructor

class TlbConstructorStub(
    parent: StubElement<*>,
    elementType: IStubElementType<*, *>,
    name: StringRef?,
) : TlbNamedStub<TlbConstructor>(parent, elementType, name) {
    constructor(
        parent: StubElement<*>,
        elementType: IStubElementType<*, *>,
        name: String?,
    ) : this(
        parent,
        elementType,
        StringRef.fromString(name),
    )

    object Type : TlbStubElementType<TlbConstructorStub, TlbConstructor>("CONSTRUCTOR") {
        val EMPTY_ARRAY = emptyArray<TlbConstructor>()
        val ARRAY_FACTORY: ArrayFactory<TlbConstructor?> = ArrayFactory {
            if (it == 0) EMPTY_ARRAY else arrayOfNulls(it)
        }

        override fun serialize(stub: TlbConstructorStub, dataStream: StubOutputStream) {
            dataStream.writeName(stub.name)
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): TlbConstructorStub {
            return TlbConstructorStub(
                parentStub,
                this,
                dataStream.readName(),
            )
        }

        override fun createStub(psi: TlbConstructor, parentStub: StubElement<out PsiElement>): TlbConstructorStub {
            return TlbConstructorStub(parentStub, this, psi.name)
        }

        override fun createPsi(stub: TlbConstructorStub): TlbConstructor {
            return TlbConstructorImpl(stub, this)
        }

        override fun indexStub(stub: TlbConstructorStub, sink: IndexSink) {
            sink.indexTlbConstructor(stub)
        }
    }
}
