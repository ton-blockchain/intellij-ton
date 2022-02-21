package com.github.andreypfau.intellijton.tlb.ide.actions

import com.github.andreypfau.intellijton.tlb.TlbIcons
import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.psi.PsiDirectory

private const val CAPTION = "TL-B Scheme"
private const val DESCRIPTION = "Creates new TL-B scheme"
private const val TLB_FILE_TEMPLATE = "TL-B empty scheme"

class TlbCreateFileAction : CreateFileFromTemplateAction(CAPTION, DESCRIPTION, TlbIcons.FILE), DumbAware {
    override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?) = CAPTION

    override fun buildDialog(
        project: Project,
        directory: PsiDirectory,
        builder: CreateFileFromTemplateDialog.Builder
    ): Unit = builder.run {
        setTitle(CAPTION)
        addKind(CAPTION, TlbIcons.FILE, TLB_FILE_TEMPLATE)
        setValidator(object : InputValidatorEx {
            override fun getErrorText(inputString: String?): String? =
                if (inputString.isNullOrEmpty()) {
                    "`$inputString` is not valid TL-B scheme name"
                } else null
        })
    }
}