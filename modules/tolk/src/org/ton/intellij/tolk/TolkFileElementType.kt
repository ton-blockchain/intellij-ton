package org.ton.intellij.tolk

import com.intellij.psi.PsiFile
import com.intellij.psi.StubBuilder
import com.intellij.psi.stubs.*
import com.intellij.psi.tree.IStubFileElementType
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.stub.TolkFileStub

private const val STUB_VERSION = 21

object TolkFileElementType : IStubFileElementType<TolkFileStub>("TOLK_FILE", TolkLanguage) {
    override fun getStubVersion(): Int = STUB_VERSION

    override fun getBuilder(): StubBuilder = object : DefaultStubBuilder() {
        override fun createStubForFile(file: PsiFile): StubElement<*> =
            if (file is TolkFile) TolkFileStub(file)
            else super.createStubForFile(file)
    }

    override fun indexStub(stub: PsiFileStub<*>, sink: IndexSink) {
        super.indexStub(stub, sink)
    }

    override fun serialize(stub: TolkFileStub, dataStream: StubOutputStream) {

    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): TolkFileStub {
        return TolkFileStub(null)
    }

    override fun getExternalId(): String = "tolk.FILE"
}
