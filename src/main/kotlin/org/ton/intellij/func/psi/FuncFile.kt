package org.ton.intellij.func.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import org.ton.intellij.func.FuncFileType
import org.ton.intellij.func.FuncLanguage
import org.ton.intellij.func.stub.FuncFileStub

class FuncFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, FuncLanguage) {
    override fun getFileType(): FileType = FuncFileType

    override fun getStub(): FuncFileStub = super.getStub() as FuncFileStub
}
