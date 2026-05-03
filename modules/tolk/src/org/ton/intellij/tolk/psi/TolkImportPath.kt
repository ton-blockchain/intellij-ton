package org.ton.intellij.tolk.psi

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import org.ton.intellij.acton.cli.ActonToml
import org.ton.intellij.tolk.ide.configurable.tolkSettings

fun computeTolkImportPath(
    project: Project,
    sourceVirtualFile: VirtualFile,
    targetVirtualFile: VirtualFile,
    actonToml: ActonToml? = ActonToml.find(project, sourceVirtualFile),
): String? {
    val sdk = project.tolkSettings.stdlibDirFor(sourceVirtualFile)
    if (sdk != null && VfsUtilCore.isAncestor(sdk, targetVirtualFile, false)) {
        val relativePath = VfsUtilCore.getRelativePath(targetVirtualFile, sdk, '/') ?: return null
        return "@stdlib/$relativePath".removeSuffix(".tolk")
    }

    shortestActonImportMappingPath(targetVirtualFile, actonToml)?.let { return it }

    val sourceDir = if (sourceVirtualFile.isDirectory) sourceVirtualFile else sourceVirtualFile.parent
    return sourceDir?.let {
        VfsUtil.findRelativePath(it, targetVirtualFile, '/')?.removeSuffix(".tolk")
    }
}

fun shortestActonImportMappingPath(targetVirtualFile: VirtualFile, actonToml: ActonToml?): String? =
    actonImportMappingPaths(targetVirtualFile, actonToml).firstOrNull()

fun actonImportMappingPaths(targetVirtualFile: VirtualFile, actonToml: ActonToml?): List<String> {
    val mappings = actonToml?.getNormalizedMappings() ?: return emptyList()
    return mappings.mapNotNull { (key, mappingDir) ->
        toMappedImportPath(targetVirtualFile, key, mappingDir)
    }.sortedWith(compareBy<String> { it.length }.thenBy { it })
}

private fun toMappedImportPath(targetVirtualFile: VirtualFile, key: String, mappingDir: String): String? {
    val normalizedFilePath = targetVirtualFile.path.replace('\\', '/')
    val normalizedMappingDir = mappingDir.replace('\\', '/').trimEnd('/')

    if (!normalizedFilePath.startsWith(normalizedMappingDir)) return null
    if (normalizedFilePath.length > normalizedMappingDir.length) {
        val separator = normalizedFilePath[normalizedMappingDir.length]
        if (separator != '/' && separator != '\\') return null
    }

    val subPath = normalizedFilePath.removePrefix(normalizedMappingDir).removePrefix("/").removePrefix("\\")
    val mappedPath = if (subPath.isEmpty()) "@$key" else "@$key/$subPath"
    return mappedPath.removeSuffix(".tolk")
}
