package org.ton.intellij.tasm.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import org.ton.intellij.tasm.TasmFileType
import org.ton.intellij.tasm.TasmLanguage

class TasmFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, TasmLanguage) {
    override fun getFileType(): FileType = TasmFileType
    override fun toString(): String = "TASM File"
}
