package com.github.andreypfau.intellijton.func.ide.actions

import com.github.andreypfau.intellijton.func.FuncIcons
import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.psi.PsiDirectory

private const val CAPTION = "FunC File"
private const val DESCRIPTION = "Creates new FunC File"
private const val FUNC_FILE_TEMPLATE = "FunC Contract"

class FuncCreateFileAction : CreateFileFromTemplateAction(CAPTION, DESCRIPTION, FuncIcons.FILE), DumbAware {
    override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?) = CAPTION

    override fun buildDialog(
        project: Project,
        directory: PsiDirectory,
        builder: CreateFileFromTemplateDialog.Builder
    ): Unit = builder.run {
        setTitle(CAPTION)
        addKind(CAPTION, FuncIcons.FILE, FUNC_FILE_TEMPLATE)
        setValidator(object : InputValidatorEx {
            override fun getErrorText(inputString: String?): String? =
                if (inputString.isNullOrEmpty()) {
                    "`$inputString` is not valid FunC name"
                } else null
        })
    }
}