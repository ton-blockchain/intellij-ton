package org.ton.intellij.boc

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.ton.intellij.tasm.TasmFileType

class BocDisassemblyPanel(
    project: Project,
    file: VirtualFile,
    onInstalled: (() -> Unit)? = null,
) : BocBasePanel(project, file, TasmFileType, onInstalled) {
    override fun runTask() = TasmService.disassemble(file.path)
}
