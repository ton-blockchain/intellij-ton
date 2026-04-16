package org.ton.intellij.tolk.acton

import com.intellij.openapi.vfs.VfsUtilCore
import org.ton.intellij.tolk.ide.configurable.tolkSettings
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.TolkNamedElement

fun TolkNamedElement.isVisibleInCompletionFrom(currentFile: TolkFile): Boolean {
    if (!currentFile.isInContractsFolder()) return true

    val symbolFile = (containingFile.originalFile as? TolkFile) ?: (containingFile as? TolkFile) ?: return true
    return !symbolFile.isActonProjectHelperFile()
}

private fun TolkFile.isInContractsFolder(): Boolean {
    val path = originalFile.virtualFile?.path ?: return false
    return path.contains("/contracts/") || path.contains("\\contracts\\")
}

private fun TolkFile.isActonProjectHelperFile(): Boolean {
    val file = virtualFile ?: return false
    val stdlibDir = project.tolkSettings.stdlibDir
    if (stdlibDir != null && VfsUtilCore.isAncestor(stdlibDir, file, false)) {
        return false
    }

    val path = file.path
    return path.contains("/.acton/") || path.contains("\\.acton\\")
}
