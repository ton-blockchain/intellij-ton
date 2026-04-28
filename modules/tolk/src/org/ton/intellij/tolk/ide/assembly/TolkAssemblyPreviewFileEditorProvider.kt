package org.ton.intellij.tolk.ide.assembly

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class TolkAssemblyPreviewFileEditorProvider :
    FileEditorProvider,
    DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean = file is TolkAssemblyPreviewVirtualFile

    override fun createEditor(project: Project, file: VirtualFile): FileEditor =
        TolkAssemblyPreviewEditor(project, file as TolkAssemblyPreviewVirtualFile)

    override fun getEditorTypeId(): String = "TolkAssemblyPreviewFileEditor"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}
