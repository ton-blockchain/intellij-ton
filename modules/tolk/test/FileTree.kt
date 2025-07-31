package org.ton.intellij.tolk

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Path
import java.nio.file.Paths

val VirtualFile.pathAsPath: Path get() = Paths.get(path)
fun fullyRefreshDirectory(directory: VirtualFile) {
    VfsUtil.markDirtyAndRefresh(/* async = */ false, /* recursive = */ true, /* reloadChildren = */ true, directory)
}

fun saveAllDocuments() = FileDocumentManager.getInstance().saveAllDocuments()

fun replaceCaretMarker(text: String): String = text.replace("/*caret*/", "<caret>")
fun hasCaretMarker(text: String): Boolean = text.contains("/*caret*/") || text.contains("<caret>")
fun replaceSelectionMarker(text: String): String = text
    .replace("/*selection*/", "<selection>")
    .replace("/*selection**/", "</selection>")

fun hasSelectionMarker(text: String): Boolean = text.contains("<selection>") && text.contains("</selection>")
