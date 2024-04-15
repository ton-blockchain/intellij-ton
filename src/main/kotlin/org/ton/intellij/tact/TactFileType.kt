package org.ton.intellij.tact

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.VirtualFile

object TactFileType : LanguageFileType(TactLanguage) {
    override fun getName() = "Tact"
    override fun getDescription() = "Tact files"
    override fun getDefaultExtension() = "tact"
    override fun getIcon() = TactIcons.FILE
    override fun getCharset(file: VirtualFile, content: ByteArray): String = Charsets.UTF_8.name()
}
