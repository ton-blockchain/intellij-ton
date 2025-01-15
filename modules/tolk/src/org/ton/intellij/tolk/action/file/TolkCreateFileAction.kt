package org.ton.intellij.tolk.action.file

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import org.ton.intellij.tolk.TolkIcons

class TolkCreateFileAction : CreateFileFromTemplateAction(
    FILE_TEMPLATE, "", TolkIcons.FILE
) {
    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder
            .setTitle(TOLK_FILE)
            .addKind("Empty file", TolkIcons.FILE, FILE_TEMPLATE)
    }

    override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?): String {
        return FILE_TEMPLATE
    }

    companion object {
        const val FILE_TEMPLATE = "Tolk File"
        const val TOLK_FILE = "New Tolk file"
    }
}
