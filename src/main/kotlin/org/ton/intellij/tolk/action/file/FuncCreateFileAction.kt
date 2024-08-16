package org.ton.intellij.tolk.action.file

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import org.ton.intellij.tolk.TolkIcons

class TolkCreateFileAction : CreateFileFromTemplateAction(
    NEW_FUNC_FILE, "", TolkIcons.FILE
) {
    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder
            .setTitle(NEW_FUNC_FILE)
            .addKind("Empty file", TolkIcons.FILE, FILE_TEMPLATE)
    }

    override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?): String {
        return NEW_FUNC_FILE
    }

    companion object {
        const val FILE_TEMPLATE = "FunC File"
        const val NEW_FUNC_FILE = "New FunC File"
    }
}
