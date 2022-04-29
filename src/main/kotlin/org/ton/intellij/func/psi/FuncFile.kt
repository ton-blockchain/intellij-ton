package org.ton.intellij.func.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import org.ton.intellij.func.FuncFileType
import org.ton.intellij.func.FuncLanguage

class FuncFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, FuncLanguage), FuncElement {
    override fun getFileType(): FileType = FuncFileType
    override fun toString(): String = "FunC"
}