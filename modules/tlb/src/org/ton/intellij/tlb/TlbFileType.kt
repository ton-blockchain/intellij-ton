package org.ton.intellij.tlb

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.NonNls

object TlbFileType : LanguageFileType(TlbLanguage) {
    override fun getName() = "TLB"
    override fun getDescription() = "TL-B schema"
    override fun getDefaultExtension() = "tlb"
    override fun getIcon() = TlbIcons.FILE
    override fun getCharset(file: VirtualFile, content: ByteArray): @NonNls String? = Charsets.UTF_8.name()
}
