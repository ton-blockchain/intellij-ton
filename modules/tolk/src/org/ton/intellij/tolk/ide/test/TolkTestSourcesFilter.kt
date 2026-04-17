package org.ton.intellij.tolk.ide.test

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.TestSourcesFilter
import com.intellij.openapi.vfs.VirtualFile
import org.ton.intellij.tolk.TolkFileType

class TolkTestSourcesFilter : TestSourcesFilter() {
    override fun isTestSource(file: VirtualFile, project: Project): Boolean {
        val fileIndex = ProjectFileIndex.getInstance(project)
        if (!fileIndex.isInContent(file)) {
            return false
        }

        if (file.isUnderTestsRoot(project)) {
            return file.isDirectory || file.fileType == TolkFileType
        }

        return file.fileType == TolkFileType && file.name.endsWith(".test.tolk")
    }
}
