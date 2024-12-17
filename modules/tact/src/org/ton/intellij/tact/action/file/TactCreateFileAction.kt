package org.ton.intellij.tact.action.file

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import org.ton.intellij.tact.TactIcons

class TactCreateFileAction : CreateFileFromTemplateAction(
    FILE_TEMPLATE, "", TactIcons.FILE
) {
    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder
            .setTitle(NEW_TACT_FILE)
            .addKind("Empty file", TactIcons.FILE, FILE_TEMPLATE)
    }

    override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?): String {
        return FILE_TEMPLATE
    }

    companion object {
        const val FILE_TEMPLATE = "Tact File"
        const val NEW_TACT_FILE = "New Tact file"
    }
}
