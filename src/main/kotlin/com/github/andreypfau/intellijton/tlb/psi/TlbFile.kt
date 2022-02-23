package com.github.andreypfau.intellijton.tlb.psi

import com.github.andreypfau.intellijton.tlb.TlbFileType
import com.github.andreypfau.intellijton.tlb.TlbLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider

class TlbFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, TlbLanguage), TlbElement {
    override fun getFileType(): FileType = TlbFileType
    override fun toString(): String = "TL-B"
}