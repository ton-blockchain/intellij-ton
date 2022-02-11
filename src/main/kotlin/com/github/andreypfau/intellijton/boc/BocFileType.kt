package com.github.andreypfau.intellijton.boc

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile

object BocFileType : FileType {
    override fun getName() = "Bag of Cells"
    override fun getDescription() = "Bag of Cells file"
    override fun getDefaultExtension() = "boc"
    override fun getIcon() = BocIcons.FILE
    override fun isBinary(): Boolean = true
    override fun isReadOnly(): Boolean = true
    override fun getCharset(file: VirtualFile, content: ByteArray?) = null
}
