package org.ton.intellij.tolk.psi.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet
import org.ton.intellij.tolk.TolkFileType
import org.ton.intellij.tolk.ide.configurable.tolkToolchain
import org.ton.intellij.tolk.psi.TolkStringLiteral

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
){
    override fun computeDefaultContexts(): Collection<PsiFileSystemItem> {
        val contexts = super.computeDefaultContexts()
        if (pathString.startsWith("@stdlib")) {
            val project = element.project
            val toolchain = project.tolkToolchain ?: return contexts
            val stdlibDir = toolchain.stdlibDir ?: return contexts
            val psiFile = PsiManager.getInstance(project).findDirectory(stdlibDir) ?: return contexts
            return listOf(psiFile as PsiFileSystemItem)
        }
        return contexts
    }

    override fun createFileReference(range: TextRange?, index: Int, text: String?): FileReference? {
        if (text == "@stdlib") {
            return FileReference(this, range, index, ".")
        }
        return super.createFileReference(range, index, text)
    }
}
