package org.ton.intellij.tolk.psi.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileInfoManager
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet
import org.ton.intellij.tolk.psi.TolkFile

class TolkFileReference(set: FileReferenceSet, range: TextRange, index: Int, text: String) : FileReference(set, range, index, text) {
    override fun createLookupItem(element: PsiElement): Any {
        val file = element as? TolkFile ?: return FileInfoManager.getFileLookupItem(element)
        if (!file.isPhysical) return FileInfoManager.getFileLookupItem(element)
        val withoutExtensions = file.name.removeSuffix(".tolk")
        val extension = if (withoutExtensions == file.name) "" else ".tolk"
        val root = rangeInElement.startOffset == 1
        val finalName = if (root) "./$withoutExtensions" else withoutExtensions

        return FileInfoManager.getFileLookupItem(file, finalName, file.getIcon(0)).withTailText(extension)
    }
}
