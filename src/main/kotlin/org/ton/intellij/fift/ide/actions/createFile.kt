package org.ton.intellij.fift.ide.actions

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.psi.PsiDirectory
import org.ton.intellij.fift.FiftIcons

private const val CAPTION = "Fift File"
private const val DESCRIPTION = "Creates new Fift File"
private const val FIFT_FILE_TEMPLATE = "Fift File"

class FiftCreateFileAction : CreateFileFromTemplateAction(CAPTION, DESCRIPTION, FiftIcons.FILE), DumbAware {
    override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?) = CAPTION

    override fun buildDialog(
        project: Project,
        directory: PsiDirectory,
        builder: CreateFileFromTemplateDialog.Builder
    ): Unit = builder.run {
        setTitle(CAPTION)
        addKind(CAPTION, FiftIcons.FILE, FIFT_FILE_TEMPLATE)
        setValidator(object : InputValidatorEx {
            override fun getErrorText(inputString: String?): String? =
                if (inputString.isNullOrEmpty()) {
                    "`$inputString` is not valid Fift name"
                } else null
        })
    }
}