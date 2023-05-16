package org.ton.intellij.func

import com.intellij.psi.PsiFile
import com.intellij.psi.StubBuilder
import com.intellij.psi.stubs.*
import com.intellij.psi.tree.IStubFileElementType
import org.ton.intellij.func.psi.FuncFile
import org.ton.intellij.func.stub.FuncFileStub

private const val VERSION = 1

object FuncFileElementType : IStubFileElementType<FuncFileStub>("FUNC_FILE", FuncLanguage) {
    override fun getStubVersion(): Int = VERSION

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

    override fun getExternalId(): String = "func.FILE"
}
