package org.ton.intellij.func.action.file

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import org.ton.intellij.func.FuncIcons

class FuncCreateFileAction : CreateFileFromTemplateAction(
    FILE_TEMPLATE, "", FuncIcons.FILE
) {
    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder
            .setTitle(NEW_FUNC_FILE)
            .addKind("Empty file", FuncIcons.FILE, FILE_TEMPLATE)
    }

    override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?): String {
        return FILE_TEMPLATE
    }

    companion object {
        const val FILE_TEMPLATE = "FunC File"
        const val NEW_FUNC_FILE = "New FunC file"
    }
}
