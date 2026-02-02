package org.ton.intellij.tolk.psi.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet
import org.ton.intellij.acton.cli.ActonToml
import org.ton.intellij.tolk.TolkFileType
import org.ton.intellij.tolk.ide.configurable.tolkSettings
import org.ton.intellij.tolk.psi.TolkStringLiteral
import kotlin.io.path.absolutePathString

private val SUITABLE_FILE_TYPES = arrayOf(TolkFileType)

class TolkLiteralFileReferenceSet(
    str: String,
    element: TolkStringLiteral,
    startOffset: Int
) : FileReferenceSet(
    str,
    element,
    startOffset,
    null,
    element.containingFile.originalFile.virtualFile.fileSystem.isCaseSensitive,
    false,
    SUITABLE_FILE_TYPES
) {
    override fun computeDefaultContexts(): Collection<PsiFileSystemItem> {
        val contexts = super.computeDefaultContexts()
        val project = element.project

        if (pathString.startsWith("@stdlib")) {
            val stdlibDir = project.tolkSettings.stdlibDir ?: return contexts
            val psiFile = PsiManager.getInstance(project).findDirectory(stdlibDir) ?: return contexts
            return listOf(psiFile as PsiFileSystemItem)
        }

        val actonToml = ActonToml.find(project) ?: return contexts
        val mappings = actonToml.getMappings()
        val firstSegment = pathString.split('/').firstOrNull() ?: return contexts
        if (!firstSegment.startsWith("@")) return contexts

        val mappingValue = mappings[firstSegment.substring(1)] ?: return contexts
        val mappingPath = actonToml.workingDir.resolve(mappingValue).normalize()
        val virtualFile = element.containingFile.originalFile.virtualFile.fileSystem.findFileByPath(mappingPath.absolutePathString()) ?: return contexts
        val psiDirectory = PsiManager.getInstance(project).findDirectory(virtualFile) ?: return contexts
        return listOf(psiDirectory as PsiFileSystemItem)
    }

    override fun createFileReference(range: TextRange, index: Int, text: String): FileReference {
        if (text.startsWith("@")) {
            return FileReference(this, range, index, ".")
        }
        if (!text.endsWith(".tolk") && range.endOffset - 1 == this.pathString.length) {
            return TolkFileReference(this, range, index, "$text.tolk")
        }
        return TolkFileReference(this, range, index, text)
    }
}
