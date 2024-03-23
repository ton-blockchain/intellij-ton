package org.ton.intellij.tlb.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import org.ton.intellij.tlb.TlbFileType
import org.ton.intellij.tlb.TlbLanguage

class TlbFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, TlbLanguage), TlbElement {
    override fun getFileType(): FileType = TlbFileType
    override fun toString(): String = "TLB"
}
