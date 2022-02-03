package com.github.andreypfau.intellijton.fift.psi

import com.github.andreypfau.intellijton.fift.FiftFileType
import com.github.andreypfau.intellijton.fift.FiftLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider

class FiftFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, FiftLanguage) {
    override fun getFileType(): FileType = FiftFileType

    override fun toString(): String = "Fift"
}