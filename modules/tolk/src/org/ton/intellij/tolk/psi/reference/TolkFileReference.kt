package org.ton.intellij.tolk.psi.reference

import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileInfoManager
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet
import org.ton.intellij.acton.cli.ActonToml
import org.ton.intellij.tolk.ide.configurable.tolkSettings
import org.ton.intellij.tolk.psi.TolkFile

class TolkFileReference(set: FileReferenceSet, range: TextRange, index: Int, text: String) :
    FileReference(set, range, index, text) {
    override fun createLookupItem(element: PsiElement): Any {
        val file = element as? TolkFile ?: return FileInfoManager.getFileLookupItem(element)
        if (!file.isPhysical) return FileInfoManager.getFileLookupItem(element)
        val withoutExtensions = file.name.removeSuffix(".tolk")
        val extension = if (withoutExtensions == file.name) "" else ".tolk"
        val finalName = withoutExtensions

        return FileInfoManager.getFileLookupItem(file, finalName, file.getIcon(0)).withTailText(extension)
    }

    override fun bindToElement(element: PsiElement, absolute: Boolean): PsiElement {
        if (isLast && fileReferenceSet.pathString.startsWith("@")) {
            val targetVirtualFile = (element as? PsiFileSystemItem)?.virtualFile
            val mappedPath = targetVirtualFile?.let { computeMappedPath(it, fileReferenceSet.pathString) }
            if (mappedPath != null) {
                return rename(mappedPath)
            }
        }
        return super.bindToElement(element, absolute)
    }

    override fun rename(newName: String): PsiElement =
        super.rename(if (isLast) fixLastNameForRename(newName) else newName)

    fun fixLastNameForRename(newName: String): String {
        if (newName.endsWith(".tolk")) {
            return newName.removeSuffix(".tolk")
        }
        return newName
    }

    private fun computeMappedPath(
        targetVirtualFile: com.intellij.openapi.vfs.VirtualFile,
        currentPath: String,
    ): String? {
        val project = element.project
        val alias = currentPath.substringAfter('@', "").substringBefore('/')
        if (alias.isEmpty()) return null

        if (alias == "stdlib") {
            val stdlibDir = project.tolkSettings.stdlibDir ?: return null
            if (!VfsUtilCore.isAncestor(stdlibDir, targetVirtualFile, false)) return null
            val relativePath = VfsUtilCore.getRelativePath(targetVirtualFile, stdlibDir, '/') ?: return null
            return "@stdlib/$relativePath".removeSuffix(".tolk")
        }

        val sourceVirtualFile = element.containingFile.originalFile.virtualFile ?: return null
        val actonToml = ActonToml.find(project, sourceVirtualFile) ?: return null
        val normalizedMappings = actonToml.getNormalizedMappings()

        val preferredPath = normalizedMappings[alias]
        if (preferredPath != null) {
            toMappedPath(targetVirtualFile, alias, preferredPath)?.let { return it }
        }

        for ((key, mappingDir) in normalizedMappings) {
            toMappedPath(targetVirtualFile, key, mappingDir)?.let { return it }
        }

        return null
    }

    private fun toMappedPath(
        targetVirtualFile: com.intellij.openapi.vfs.VirtualFile,
        key: String,
        mappingDir: String,
    ): String? {
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
}
