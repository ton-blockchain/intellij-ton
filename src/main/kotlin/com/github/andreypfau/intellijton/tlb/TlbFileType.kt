package com.github.andreypfau.intellijton.tlb

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.VirtualFile

object TlbFileType : LanguageFileType(TlbLanguage) {
    override fun getName() = "TL-B"
    override fun getDescription() = "TL-B Schema file"
    override fun getDefaultExtension() = "tlb"
    override fun getIcon() = TlbIcons.FILE
    override fun getCharset(file: VirtualFile, content: ByteArray?) = Charsets.UTF_8.name()
}
