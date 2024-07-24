package org.ton.intellij.tlb.stub

import com.intellij.psi.PsiFile
import com.intellij.psi.StubBuilder
import com.intellij.psi.stubs.*
import com.intellij.psi.tree.IStubFileElementType
import org.ton.intellij.tlb.TlbLanguage
import org.ton.intellij.tlb.psi.TlbFile

class TlbFileStub(
    file: TlbFile?,
) : PsiFileStubImpl<TlbFile>(file) {
    override fun getType(): Type = Type

    object Type : IStubFileElementType<TlbFileStub>(TlbLanguage) {
        private const val STUB_VERSION = 1

        override fun getStubVersion(): Int = STUB_VERSION

        override fun getBuilder(): StubBuilder = object : DefaultStubBuilder() {
            override fun createStubForFile(file: PsiFile): StubElement<*> =
                if (file is TlbFile) TlbFileStub(file)
                else super.createStubForFile(file)
        }

        override fun indexStub(stub: PsiFileStub<*>, sink: IndexSink) {
            super.indexStub(stub, sink)
        }

        override fun serialize(stub: TlbFileStub, dataStream: StubOutputStream) {

        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): TlbFileStub {
            return TlbFileStub(null)
        }

        override fun getExternalId(): String = "tlb.FILE"
    }
}
