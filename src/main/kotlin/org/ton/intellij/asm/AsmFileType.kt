package org.ton.intellij.asm

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object AsmFileType : LanguageFileType(AsmLanguage) {
    override fun getName(): String = "TVM assembly"

    override fun getDescription(): String = AsmBundle.message("filetype.asm.description")

    override fun getDefaultExtension(): String = "tvm"

    override fun getIcon(): Icon? = null
}
