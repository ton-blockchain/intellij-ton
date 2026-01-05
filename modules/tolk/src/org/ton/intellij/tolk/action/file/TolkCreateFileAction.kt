package org.ton.intellij.tolk.action.file

import com.intellij.execution.ExecutionException
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.ui.EditorNotifications
import org.ton.intellij.acton.cli.ActonCommand
import org.ton.intellij.acton.cli.ActonCommandLine
import org.ton.intellij.acton.cli.ActonToml
import org.ton.intellij.tolk.TolkIcons

class TolkCreateFileAction : CreateFileFromTemplateAction(
    FILE_TEMPLATE, "", TolkIcons.FILE
) {
    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder
            .setTitle(TOLK_FILE)
            .addKind("Empty file", TolkIcons.FILE, FILE_TEMPLATE)
            .addKind("Contract", TolkIcons.FILE, CONTRACT_TEMPLATE)
    }

    override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?): String {
        return templateName ?: FILE_TEMPLATE
    }

    override fun postProcess(createdElement: PsiFile, templateName: String?, settings: Map<String, String>?) {
        super.postProcess(createdElement, templateName, settings)
        if (templateName == CONTRACT_TEMPLATE) {
            val project = createdElement.project
            val file = createdElement.virtualFile ?: return
            val actonToml = ActonToml.find(project) ?: return
            val relativePath = VfsUtil.getRelativePath(file, actonToml.virtualFile.parent) ?: return

            val command = ActonCommand.InternalRegisterContract(path = relativePath)
            val commandLine = ActonCommandLine(
                command = command.name,
                workingDirectory = actonToml.workingDir,
                additionalArguments = command.getArguments()
            ).toGeneralCommandLine(project)

            ApplicationManager.getApplication().executeOnPooledThread {
                try {
                    val handler = CapturingProcessHandler(commandLine)
                    val output = handler.runProcess()
                    if (output.exitCode == 0) {
                        ApplicationManager.getApplication().invokeLater {
                            EditorNotifications.getInstance(project).updateNotifications(file)
                            VfsUtil.markDirtyAndRefresh(true, true, true, actonToml.virtualFile)
                        }
                    }
                } catch (_: ExecutionException) {
                    // skipped
                }
            }
        }
    }

    companion object {
        const val FILE_TEMPLATE = "Tolk File"
        const val CONTRACT_TEMPLATE = "Tolk Contract"
        const val TOLK_FILE = "New Tolk file"
    }
}
