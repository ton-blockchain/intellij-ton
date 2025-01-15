package org.ton.intellij.fift

import com.intellij.openapi.fileTypes.LanguageFileType

object FiftFileType : LanguageFileType(FiftLanguage) {
    override fun getName() = "Fift"
    override fun getDescription() = "Fift"
    override fun getDefaultExtension() = "fif"
    override fun getIcon() = FiftIcons.FILE
}
