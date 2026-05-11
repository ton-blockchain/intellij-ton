package org.ton.intellij.tolk.acton

import com.intellij.openapi.vfs.VfsUtilCore
import org.ton.intellij.acton.cli.ActonToml
import org.ton.intellij.tolk.ide.configurable.tolkSettings
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.TolkNamedElement

fun TolkNamedElement.isVisibleInCompletionFrom(currentFile: TolkFile): Boolean {
    if (!currentFile.isInContractSourceScope()) return true

    val symbolFile = (containingFile.originalFile as? TolkFile) ?: (containingFile as? TolkFile) ?: return true
    return !symbolFile.isActonProjectHelperFile(currentFile)
}

private fun TolkFile.isInContractSourceScope(): Boolean {
    val path = originalFile.virtualFile?.path ?: return false
    ActonToml.find(project, originalFile.virtualFile)?.getNormalizedMappings()?.get("contracts")?.let { contractsDir ->
        return path.isSameOrUnder(contractsDir)
    }

    return path.contains("/contracts/") || path.contains("\\contracts\\")
}

private fun TolkFile.isActonProjectHelperFile(currentFile: TolkFile): Boolean {
    val file = virtualFile ?: return false
    val stdlibDir = project.tolkSettings.stdlibDirFor(currentFile.originalFile.virtualFile)
    if (stdlibDir != null && VfsUtilCore.isAncestor(stdlibDir, file, false)) {
        return false
    }

    val path = file.path
    return path.contains("/.acton/") || path.contains("\\.acton\\")
}

private fun String.isSameOrUnder(rootPath: String): Boolean {
    val path = replace('\\', '/').trimEnd('/')
    val root = rootPath.replace('\\', '/').trimEnd('/')
    return path == root || path.startsWith("$root/")
}
