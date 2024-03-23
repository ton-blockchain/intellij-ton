package org.ton.intellij.tlb.ide.actions

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.ProjectScope
import com.intellij.util.IncorrectOperationException
import io.ktor.utils.io.errors.*
import org.ton.intellij.tlb.psi.TlbFile
import org.ton.tlb.compiler.TlbCompiler
import org.ton.tlb.generator.FuncCodeGen
import org.ton.tlb.parser.TlbGrammar

class GenerateFuncTlbAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.PSI_FILE)
        e.presentation.isEnabledAndVisible = file is TlbFile
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.PSI_FILE) as? TlbFile ?: return
        val funcTlbParserFileName = getFuncTlbParserFileName(file)
        val files =
            FilenameIndex.getVirtualFilesByName(funcTlbParserFileName, ProjectScope.getAllScope(project)).firstOrNull()
        val baseDir = files?.parent
            ?: FilenameIndex.getVirtualFilesByName("stdlib.fc", ProjectScope.getAllScope(project)).firstOrNull()?.parent
            ?: file.virtualFile.parent

        val descriptor = FileSaverDescriptor("Save TL-B Parser", "", "fc")
        val virtualFile = FileChooserFactory.getInstance()
            .createSaveFileDialog(descriptor, project)
            .save(baseDir, funcTlbParserFileName)
            ?.getVirtualFile(true) ?: return

        WriteCommandAction.runWriteCommandAction(project, e.presentation.text, null, Runnable {
            try {
                val text = generateFuncTlbParser(file)
                VfsUtil.saveText(virtualFile, text)
                Notifications.Bus.notify(
                    Notification(
                        "TL-B Generator",
                        "${virtualFile.name} generated",
                        "to ${virtualFile.parent.path}",
                        NotificationType.INFORMATION
                    ),
                    project
                )
                FileEditorManager.getInstance(project).openFile(virtualFile, false, true)
            } catch (e: Throwable) {
                if (e is IOException || e is IncorrectOperationException) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project,
                            "Failed to create file $funcTlbParserFileName\n${e.localizedMessage}",
                            "Create FunC TL-B Parser"
                        )
                    }
                } else {
                    throw e
                }
            }
        })
    }

    companion object {
        fun generateFuncTlbParser(file: TlbFile): String {
            val input = file.text
            val ast = TlbGrammar().parseOrThrow(input)
            val compiler = TlbCompiler()
            ast.forEach {
                compiler.compileConstructor(it)
            }
            val output = buildString {
                appendLine(FuncCodeGen.TLB_LIB)
                compiler.types.values.forEach { type ->
                    val funcCodeGen = FuncCodeGen(type)
                    funcCodeGen.generate(this)
                }
            }
            return output
        }

        fun getFuncTlbParserFileName(tlbFile: TlbFile): String {
            val name = tlbFile.name
            return "$name.fc"
        }
    }
}
