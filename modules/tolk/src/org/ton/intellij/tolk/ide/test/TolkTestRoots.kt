package org.ton.intellij.tolk.ide.test

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile

internal fun VirtualFile.isTestsRoot(project: Project): Boolean = isDirectory &&
    name == "tests" &&
    ProjectFileIndex.getInstance(project).isInContent(this)

internal fun VirtualFile.isSourceRoot(project: Project): Boolean {
    if (!isDirectory || name !in SOURCE_ROOT_NAMES) {
        return false
    }

    val fileIndex = ProjectFileIndex.getInstance(project)
    return fileIndex.isInContent(this) && parent == fileIndex.getContentRootForFile(this)
}

internal fun VirtualFile.isUnderTestsRoot(project: Project): Boolean {
    var current: VirtualFile? = this
    while (current != null) {
        if (current.isTestsRoot(project)) {
            return true
        }
        current = current.parent
    }
    return false
}

private val SOURCE_ROOT_NAMES = setOf("contracts", "src")
