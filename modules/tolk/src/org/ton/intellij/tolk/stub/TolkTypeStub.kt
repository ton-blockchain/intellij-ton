package org.ton.intellij.tolk.stub

import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.ton.intellij.tolk.psi.TolkElement
import org.ton.intellij.tolk.stub.type.TolkStubElementType

class TolkTypeStub<PsiT : TolkElement>(parent: StubElement<*>?, elementType: IStubElementType<*, *>?, ref: StringRef?) :
    StubWithText<PsiT>(parent, elementType, ref) {

    class Type<PsiT : TolkElement>(
        name: String,
        private val psiCtor: (TolkTypeStub<*>, IStubElementType<*, *>) -> PsiT,
    ) : TolkStubElementType<TolkTypeStub<PsiT>, PsiT>(name) {

        override fun createPsi(stub: TolkTypeStub<PsiT>): PsiT = psiCtor(stub, this)
        override fun createStub(psi: PsiT, parentStub: StubElement<*>?): TolkTypeStub<PsiT> =
            TolkTypeStub(parentStub, this, StringRef.fromString(psi.text))

        override fun serialize(stub: TolkTypeStub<PsiT>, dataStream: StubOutputStream) {
            dataStream.writeName(stub.getText())
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): TolkTypeStub<PsiT> {
            return TolkTypeStub(parentStub, this, dataStream.readName())
        }
    }
}