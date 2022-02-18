package com.github.andreypfau.intellijton.tlb.psi

import com.github.andreypfau.intellijton.func.FuncFileType
import com.github.andreypfau.intellijton.func.FuncLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider

class TlbFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, FuncLanguage), TlbElement {
    override fun getFileType(): FileType = FuncFileType
    override fun toString(): String = "TL-B"
}