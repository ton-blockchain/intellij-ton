package org.ton.intellij.boc

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class BocBase64Panel(
    project: Project,
    file: VirtualFile,
    onInstalled: (() -> Unit)? = null,
) : BocBasePanel(project, file, onInstalled) {
    override fun runTask() = TasmService.readFileAsBase64(file.path)
}
