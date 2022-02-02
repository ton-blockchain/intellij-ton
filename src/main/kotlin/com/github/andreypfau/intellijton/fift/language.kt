package com.github.andreypfau.intellijton.fift

import com.github.andreypfau.intellijton.func.FuncIcons
import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.VirtualFile

object FiftLanguage : Language("Fift", "text/fift")

object FiftFileType : LanguageFileType(FiftLanguage) {
    override fun getName() = "Fift"
    override fun getDescription() = "Fift language file"
    override fun getDefaultExtension() = "fif"
    override fun getIcon() = FiftIcons.FILE
    override fun getCharset(file: VirtualFile, content: ByteArray?) = Charsets.UTF_8.name()
}
