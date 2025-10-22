package org.ton.intellij.tolk.ide.test

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.TestSourcesFilter
import com.intellij.openapi.vfs.VirtualFile
import org.ton.intellij.tolk.TolkFileType

class TolkTestSourcesFilter : TestSourcesFilter() {
    override fun isTestSource(file: VirtualFile, project: Project): Boolean {
        if (file.fileType != TolkFileType) {
            return false
        }

        if (!ProjectFileIndex.getInstance(project).isInContent(file)) {
            return false
        }

        // Consider files with "test" in name as test files
        return file.name.contains("test") || file.name.contains("Test")
    }
}
