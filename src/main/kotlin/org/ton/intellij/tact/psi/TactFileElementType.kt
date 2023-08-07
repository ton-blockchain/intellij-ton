package org.ton.intellij.tact.psi

import com.intellij.psi.PsiFile
import com.intellij.psi.StubBuilder
import com.intellij.psi.stubs.*
import com.intellij.psi.tree.IStubFileElementType
import org.ton.intellij.func.psi.FuncFile
import org.ton.intellij.func.stub.FuncFileStub
import org.ton.intellij.tact.TactLanguage

private const val STUB_VERSION = 1

object TactFileElementType : IStubFileElementType<FuncFileStub>("FUNC_FILE", TactLanguage) {
    override fun getStubVersion(): Int = STUB_VERSION

    override fun getBuilder(): StubBuilder = object : DefaultStubBuilder() {
        override fun createStubForFile(file: PsiFile): StubElement<*> =
            if (file is FuncFile) FuncFileStub(file)
            else super.createStubForFile(file)
    }

    override fun indexStub(stub: PsiFileStub<*>, sink: IndexSink) {
        super.indexStub(stub, sink)
    }

    override fun serialize(stub: FuncFileStub, dataStream: StubOutputStream) {

    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): FuncFileStub {
        return FuncFileStub(null)
    }

    override fun getExternalId(): String = "tact.FILE"
}
