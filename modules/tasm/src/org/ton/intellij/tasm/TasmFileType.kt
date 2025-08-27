package org.ton.intellij.tasm

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object TasmFileType : LanguageFileType(TasmLanguage) {
    override fun getName() = "TASM"
    override fun getDescription() = "TON assembly language"
    override fun getDefaultExtension() = "tasm"
    override fun getIcon(): Icon = TasmIcons.FILE
}
