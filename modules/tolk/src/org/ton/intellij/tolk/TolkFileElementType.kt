package org.ton.intellij.tolk

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiFile
import com.intellij.psi.StubBuilder
import com.intellij.psi.stubs.*
import com.intellij.psi.tree.IStubFileElementType
import org.ton.intellij.tolk.psi.TOLK_COMMENTS
import org.ton.intellij.tolk.psi.TolkElementTypes
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.stub.TolkFileStub

private const val STUB_VERSION = 41

object TolkFileElementType : IStubFileElementType<TolkFileStub>("TOLK_FILE", TolkLanguage) {
    override fun getStubVersion(): Int = STUB_VERSION

    override fun getBuilder(): StubBuilder = object : DefaultStubBuilder() {
        override fun createStubForFile(file: PsiFile): StubElement<*> = if (file is TolkFile) TolkFileStub(file)
        else super.createStubForFile(file)

        override fun skipChildProcessingWhenBuildingStubs(parent: ASTNode, node: ASTNode): Boolean {
            return node.elementType == TolkElementTypes.BLOCK_STATEMENT || node.elementType in TOLK_COMMENTS
        }
    }

    override fun indexStub(stub: PsiFileStub<*>, sink: IndexSink) {
        super.indexStub(stub, sink)
    }

    override fun serialize(stub: TolkFileStub, dataStream: StubOutputStream) {}

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): TolkFileStub {
        return TolkFileStub(null)
    }

//    Uncomment to find out what causes switch to the AST
//    private val PARESED = com.intellij.util.containers.ContainerUtil.newConcurrentSet<String>()
//    override fun doParseContents(chameleon: ASTNode, psi: com.intellij.psi.PsiElement): ASTNode? {
//        val path = psi.containingFile?.virtualFile?.path
//        if (path != null && PARESED.add(path)) {
//            println("Parsing (${PARESED.size}) $path")
//            val trace = java.io.StringWriter().also { writer ->
//                Exception().printStackTrace(java.io.PrintWriter(writer))
//                writer.toString()
//            }.toString()
//            if (!trace.contains("PsiSearchHelperImpl.lambda\$processPsiFileRoots$8")) {
//                println(trace)
//                println()
//            }
//        }
//        return super.doParseContents(chameleon, psi)
//    }

    override fun getExternalId(): String = "tolk.FILE"
}
