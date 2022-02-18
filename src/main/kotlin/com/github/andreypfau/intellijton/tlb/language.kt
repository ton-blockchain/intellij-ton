package com.github.andreypfau.intellijton.tlb

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.VirtualFile

object TlbLanguage : Language("TL-B", "text/tlb")

object TlbFileType : LanguageFileType(TlbLanguage) {
    override fun getName() = "TL-B"
    override fun getDescription() = "TL-B Scheme file"
    override fun getDefaultExtension() = "tlb"
    override fun getIcon() = TlbIcons.FILE
    override fun getCharset(file: VirtualFile, content: ByteArray?) = Charsets.UTF_8.name()
}
