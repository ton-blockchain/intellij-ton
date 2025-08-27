package org.ton.intellij.boc

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class BocHexPanel(
    project: Project,
    file: VirtualFile,
    onInstalled: (() -> Unit)? = null,
) : BocBasePanel(project, file, onInstalled = onInstalled) {
    override fun runTask() = TasmService.readFileAsHex(file.path)
}
