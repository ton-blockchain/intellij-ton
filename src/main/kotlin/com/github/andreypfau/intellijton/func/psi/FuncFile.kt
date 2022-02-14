package com.github.andreypfau.intellijton.func.psi

import com.github.andreypfau.intellijton.func.FuncFileType
import com.github.andreypfau.intellijton.func.FuncLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider

class FuncFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, FuncLanguage), FuncElement {
    override fun getFileType(): FileType = FuncFileType
    override fun toString(): String = "FunC"
}