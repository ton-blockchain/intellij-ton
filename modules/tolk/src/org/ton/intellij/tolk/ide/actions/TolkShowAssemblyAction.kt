package org.ton.intellij.tolk.ide.actions

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessAdapter
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessNotCreatedException
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import javax.swing.SwingConstants
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.testFramework.LightVirtualFile
import org.ton.intellij.tasm.TasmFileType
import org.ton.intellij.tolk.TolkFileType
import org.ton.intellij.acton.cli.ActonCommand
import org.ton.intellij.acton.cli.ActonCommandLine
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class TolkShowAssemblyAction : AnAction("Show Assembly") {
    private val log = logger<TolkShowAssemblyAction>()

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        val vFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val enabled = project != null && vFile != null && vFile.fileType == TolkFileType
        e.presentation.isEnabledAndVisible = enabled
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        val lightFile = LightVirtualFile("${vFile.nameWithoutExtension}.tasm", TasmFileType, "// Compiling...")
        openInRightSplit(project, lightFile)

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Compiling ${vFile.name}") {
            override fun run(indicator: ProgressIndicator) {
                val result = compileAndDisassemble(project, vFile.path)
                ApplicationManager.getApplication().invokeLater {
                    runWriteAction {
                        val doc = FileDocumentManager.getInstance().getDocument(lightFile)
                        if (result.isSuccess) {
                            doc?.setText(result.getOrNull() ?: "")
                        } else {
                            val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                            val cleanedMessage = stripAnsiCodes(errorMessage).trim()
                            val commentedError = cleanedMessage.lines().joinToString("\n") { "// $it" }
                            doc?.setText(commentedError)
                        }
                    }
                }
            }
        })
    }

    private fun openInRightSplit(project: Project, file: LightVirtualFile) {
        ApplicationManager.getApplication().invokeLater {
            val fileEditorManagerEx = FileEditorManagerEx.getInstanceEx(project)
            val currentWindow = fileEditorManagerEx.currentWindow ?: return@invokeLater
            currentWindow.split(SwingConstants.VERTICAL, false, file, focusNew = true, fileIsSecondaryComponent = true)
        }
    }

    private fun compileAndDisassemble(project: Project, sourcePath: String): Result<String> {
        val workingDir = project.guessProjectDir()?.toNioPath()
            ?: return Result.failure(IllegalStateException("Cannot determine project directory"))

        val compileCommand = ActonCommand.Compile(path = sourcePath, base64Only = true)
        val compileCommandLine = ActonCommandLine(
            command = compileCommand.name,
            workingDirectory = workingDir,
            additionalArguments = compileCommand.getArguments()
        ).toGeneralCommandLine(project)

        val compileResult = runExternal(compileCommandLine)
            .getOrElse {
                return Result.failure(IllegalStateException("Failed to compile file '$sourcePath': ${it.message}"))
            }

        val base64Code = compileResult.trim()
        if (base64Code.isEmpty()) {
            return Result.failure(IllegalStateException("Compilation succeeded but produced empty output"))
        }

        val disasmCommand = ActonCommand.Disasm(string = base64Code)
        val disasmCommandLine = ActonCommandLine(
            command = disasmCommand.name,
            workingDirectory = workingDir,
            additionalArguments = disasmCommand.getArguments()
        ).toGeneralCommandLine(project)

        val disasmResult = runExternal(disasmCommandLine)
            .getOrElse {
                return Result.failure(IllegalStateException("Failed to disassemble compiled code: ${it.message}"))
            }

        return Result.success(disasmResult)
    }

    private fun runExternal(cmd: GeneralCommandLine): Result<String> {
        val handler = try {
            OSProcessHandler(cmd)
        } catch (e: ProcessNotCreatedException) {
            return Result.failure(e)
        }

        var exitCode: Int? = null
        var stdoutMsg = ""
        var stderrMsg = ""
        handler.addProcessListener(object : CapturingProcessAdapter() {
            override fun processTerminated(event: ProcessEvent) {
                exitCode = event.exitCode
                stdoutMsg = output.stdout
                stderrMsg = output.stderr
            }
        })

        return try {
            handler.startNotify()
            val future = ApplicationManager.getApplication().executeOnPooledThread { handler.waitFor() }
            future.get(30, TimeUnit.SECONDS)
            if (exitCode == 0) {
                Result.success(stdoutMsg)
            } else {
                val stdoutTrimmed = stripAnsiCodes(stdoutMsg.trim())
                val stderrTrimmed = stripAnsiCodes(stderrMsg.trim())
                val errorMsg = when {
                    stderrTrimmed.isNotEmpty() && stdoutTrimmed.isNotEmpty() -> "$stderrTrimmed\n\n$stdoutTrimmed"
                    stderrTrimmed.isNotEmpty()                               -> stderrTrimmed
                    stdoutTrimmed.isNotEmpty()                               -> stdoutTrimmed
                    else                                                     -> "Exit code $exitCode"
                }
                val message = "Exit code $exitCode\n\n$errorMsg"
                log.warn("Command failed: ${cmd.commandLineString}\n$message")
                Result.failure(IllegalStateException(message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            handler.destroyProcess()
        }
    }

    private fun stripAnsiCodes(text: String): String {
        val ansiPattern = Pattern.compile("\u001B\\[[\\d;]*[A-Za-z]")
        return ansiPattern.matcher(text).replaceAll("")
    }
}
