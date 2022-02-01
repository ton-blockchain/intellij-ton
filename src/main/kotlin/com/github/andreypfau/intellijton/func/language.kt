package com.github.andreypfau.intellijton.func

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.VirtualFile

object FuncLanguage : Language("FunC", "text/func")

object FuncFileType : LanguageFileType(FuncLanguage) {
    override fun getName() = "FunC"
    override fun getDescription() = "FunC language file"
    override fun getDefaultExtension() = "fc"
    override fun getIcon() = FuncIcons.FILE
    override fun getCharset(file: VirtualFile, content: ByteArray?) = Charsets.UTF_8.name()
}
