package org.ton.intellij.tlb.ide.actions

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.psi.PsiDirectory
import org.ton.intellij.tlb.TlbIcons

class TlbCreateFileAction : CreateFileFromTemplateAction(
    "TL-B Schema", "Creates new TL-B schema", TlbIcons.FILE
), DumbAware {
    override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?) = "TL-B Schema"

    override fun buildDialog(
        project: Project,
        directory: PsiDirectory,
        builder: CreateFileFromTemplateDialog.Builder,
    ): Unit = builder.run {
        setTitle("New TL-B Schema")
        addKind("Empty file", TlbIcons.FILE, "TL-B empty schema")
        setValidator(object : InputValidatorEx {
            override fun getErrorText(inputString: String?): String? =
                if (inputString.isNullOrEmpty()) {
                    "`$inputString` is not valid TL-B schema name"
                } else null
        })
    }
}
