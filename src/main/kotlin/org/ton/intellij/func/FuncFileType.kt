package org.ton.intellij.func

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.VirtualFile

object FuncFileType : LanguageFileType(FuncLanguage) {
    override fun getName() = "func"
    override fun getDescription() = "FunC files"
    override fun getDefaultExtension() = "fc"
    val extensions = setOf("func", "fc")
    override fun getIcon() = FuncIcons.FILE
    override fun getCharset(file: VirtualFile, content: ByteArray): String = Charsets.UTF_8.name()
}
