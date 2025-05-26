package org.ton.intellij.boc

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.ton.bytecode.disassembleBoc
import org.ton.bytecode.prettyPrint

const val BOC_EDITOR_TYPE_ID = "org.ton.intellij.boc.ui.editor"

class BocFileEditorProvider : FileEditorProvider, DumbAware {

    override fun accept(project: Project, file: VirtualFile): Boolean {
        return file.fileType is BocFileType
    }

    override fun acceptRequiresReadAction() = false

    override fun getEditorTypeId(): String {
        return BOC_EDITOR_TYPE_ID
    }

    override fun getPolicy(): FileEditorPolicy {
        @Suppress("UnstableApiUsage")
        return FileEditorPolicy.HIDE_OTHER_EDITORS
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val byteContents = file.contentsToByteArray()
        val tempPreviewFile = kotlin.io.path.createTempFile("tvm_", ".tvm").toFile()
        val disassembledText =
            try {
                val disassembledFile = disassembleBoc(byteContents)
                disassembledFile.prettyPrint(includeTvmCell = false)
            } catch (e: Exception) {
                "Exception while disassembling!\n\n$e"
            }

        tempPreviewFile.writeText(disassembledText)
        val previewVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempPreviewFile)
        if (previewVirtualFile == null) {
            return createTextEditor(project, file)
        }
        val tempMainFile = kotlin.io.path.createTempFile("boc_", ".hex").toFile()
        tempMainFile.writeText(byteContents.toHexString())
        val mainVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempMainFile)
        if (mainVirtualFile == null) {
            return createTextEditor(project, file)
        }

        val mainEditor = createTextEditor(project, mainVirtualFile)
        val preview = createTextEditor(project, previewVirtualFile)
        return TextEditorWithPreview(mainEditor, preview)
    }

    private fun createTextEditor(project: Project, file: VirtualFile): TextEditor {
        return TextEditorProvider.getInstance().createEditor(project, file) as TextEditor
    }

}