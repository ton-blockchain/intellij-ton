package org.ton.intellij.func.stub

import com.intellij.psi.stubs.PsiFileStubImpl
import org.ton.intellij.func.FuncFileElementType
import org.ton.intellij.func.psi.FuncFile

class FuncFileStub(
    file: FuncFile?,
) : PsiFileStubImpl<FuncFile>(file) {
    override fun getType() = FuncFileElementType
}
