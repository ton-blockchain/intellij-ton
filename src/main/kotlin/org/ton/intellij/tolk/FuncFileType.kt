package org.ton.intellij.tolk

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.VirtualFile

object TolkFileType : LanguageFileType(TolkLanguage) {
    override fun getName() = "Tolk"
    override fun getDescription() = "Tolk files"
    override fun getDefaultExtension() = "tolk"
    override fun getIcon() = TolkIcons.FILE
    override fun getCharset(file: VirtualFile, content: ByteArray): String = Charsets.UTF_8.name()
}
