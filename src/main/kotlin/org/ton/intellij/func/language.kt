package org.ton.intellij.func

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.VirtualFile

object FuncLanguage : Language("FunC", "text/func")

object FuncFileType : LanguageFileType(FuncLanguage) {
    override fun getName() = "func"
    override fun getDescription() = "FunC language file"
    override fun getDefaultExtension() = "func"
    val extensions = setOf("func", "fc")
    override fun getIcon() = FuncIcons.FILE
    override fun getCharset(file: VirtualFile, content: ByteArray?) = Charsets.UTF_8.name()
}
