package org.ton.intellij.boc

import com.intellij.openapi.fileTypes.FileType

object BocFileType : FileType {
    override fun getName() = "Bag of Cells"
    override fun getDescription() = "Bag of Cells file"
    override fun getDefaultExtension() = "boc"
    override fun getIcon() = BocIcons.FILE
    override fun isBinary(): Boolean = true
    override fun isReadOnly(): Boolean = true
}
