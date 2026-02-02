package org.ton.intellij.tolk.ide.notification

import com.intellij.execution.ExecutionException
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationPanel.Status
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import org.ton.intellij.acton.cli.ActonCommand
import org.ton.intellij.acton.cli.ActonCommandLine
import org.ton.intellij.acton.cli.ActonToml
import org.ton.intellij.tolk.TolkBundle
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.isTolkFile
import java.util.function.Function
import javax.swing.JComponent

class TolkUnregisteredContractNotificationProvider(
    private val project: Project,
) : EditorNotificationProvider {

    override fun collectNotificationData(
        project: Project,
        file: VirtualFile,
    ): Function<in FileEditor, out JComponent?>? {
        if (!file.isTolkFile) return null

        val tolkFile = PsiManager.getInstance(project).findFile(file) as? TolkFile ?: return null
        if (tolkFile.isTestFile() || tolkFile.isInScriptsFolder() || tolkFile.isActonFile()) return null

        // only show for files with onInternalMessage
        if (tolkFile.functions.none { it.name == "onInternalMessage" }) return null

        val actonToml = ActonToml.find(project) ?: return null

        val relativePath = VfsUtil.getRelativePath(file, actonToml.virtualFile.parent) ?: return null
        if (actonToml.getContractSources().contains(relativePath)) return null

        if (isIgnored(file)) return null

        return Function { fileEditor ->
            EditorNotificationPanel(fileEditor, Status.Warning).apply {
                text = TolkBundle.message("notification.contract.not.registered")

                createActionLabel(TolkBundle.message("notification.action.register")) {
                    registerContract(project, actonToml, file, relativePath)
                }

                createActionLabel(TolkBundle.message("notification.action.ignore")) {
                    ignoreFile(file)
                    EditorNotifications.getInstance(project).updateNotifications(file)
                }
            }
        }
    }

    private fun registerContract(project: Project, actonToml: ActonToml, file: VirtualFile, relativePath: String) {
        val command = ActonCommand.InternalRegisterContract(path = relativePath)
        val commandLine = ActonCommandLine(
            command = command.name,
            workingDirectory = actonToml.workingDir,
            additionalArguments = command.getArguments()
        ).toGeneralCommandLine(project) ?: return

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val handler = CapturingProcessHandler(commandLine)
                val output = handler.runProcess()
                if (output.exitCode == 0) {
                    ApplicationManager.getApplication().invokeLater {
                        EditorNotifications.getInstance(project).updateNotifications(file)
                        // refresh Acton.toml in PSI
                        VfsUtil.markDirtyAndRefresh(true, true, true, actonToml.virtualFile)
                    }
                }
            } catch (_: ExecutionException) {
                // skipped
            }
        }
    }

    private fun ignoreFile(file: VirtualFile) {
        val ignoredFiles = getIgnoredFiles().toMutableSet()
        ignoredFiles.add(file.path)
        PropertiesComponent.getInstance(project).setList(IGNORED_FILES_KEY, ignoredFiles.toList())
    }

    private fun isIgnored(file: VirtualFile): Boolean {
        return getIgnoredFiles().contains(file.path)
    }

    private fun getIgnoredFiles(): List<String> {
        return PropertiesComponent.getInstance(project).getList(IGNORED_FILES_KEY) ?: listOf()
    }

    private companion object {
        private const val IGNORED_FILES_KEY = "org.ton.tolk.unregisteredContracts.ignored"
    }
}
