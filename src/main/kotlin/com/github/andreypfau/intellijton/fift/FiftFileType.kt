package com.github.andreypfau.intellijton.fift

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.VirtualFile

object FiftFileType : LanguageFileType(FiftLanguage) {
    override fun getName() = "Fift"
    override fun getDescription() = "Fift language file"
    override fun getDefaultExtension() = "fif"
    override fun getIcon() = FiftIcons.FILE
    override fun getCharset(file: VirtualFile, content: ByteArray?) = Charsets.UTF_8.name()
}
