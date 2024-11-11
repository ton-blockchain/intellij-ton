package org.ton.intellij.asm

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.Language
import com.intellij.psi.FileViewProvider

class AsmFile(viewProvider: FileViewProvider, language: Language) : PsiFileBase(viewProvider, language) {
    override fun getFileType(): AsmFileType = AsmFileType
}
