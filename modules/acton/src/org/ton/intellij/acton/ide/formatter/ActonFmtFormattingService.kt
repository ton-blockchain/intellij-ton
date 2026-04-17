package org.ton.intellij.acton.ide.formatter

import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.execution.process.CapturingProcessAdapter
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.formatting.service.AsyncDocumentFormattingService
import com.intellij.formatting.service.AsyncFormattingRequest
import com.intellij.formatting.service.FormattingService.Feature
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.logger
import com.intellij.psi.PsiFile
import com.intellij.psi.formatter.FormatterUtil
import org.ton.intellij.acton.ActonBundle
import org.ton.intellij.acton.cli.ActonCommandLine
import org.ton.intellij.acton.settings.actonSettings
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths

private const val ACTON_FMT_NOTIFICATION_GROUP_ID = "Acton Formatter"

class ActonFmtFormattingService : AsyncDocumentFormattingService() {
    override fun canFormat(file: PsiFile): Boolean {
        if (file.virtualFile?.extension != "tolk") return false
        return file.project.actonSettings.useActonFmtForTolkFormatting &&
            getFormattingReason() == FormattingReason.ReformatCode &&
            isActonAvailable(file)
    }

    override fun getFeatures(): MutableSet<Feature> = mutableSetOf()

    override fun getNotificationGroupId(): String = ACTON_FMT_NOTIFICATION_GROUP_ID

    override fun getName(): String = ActonBundle.message("acton.fmt.name")

    override fun createFormattingTask(request: AsyncFormattingRequest): FormattingTask? {
        val inputFile = createFormattingInputFile(request) ?: return null

        val commandLine = createCommandLine(request, inputFile.file)
        if (commandLine == null) {
            deleteTempFile(inputFile.file)
            request.onError(
                ActonBundle.message("acton.fmt.error.title"),
                ActonBundle.message("acton.fmt.error.acton.not.found"),
            )
            return null
        }

        return try {
            val handler = OSProcessHandler(commandLine.withCharset(Charsets.UTF_8))
            object : FormattingTask {
                override fun run() {
                    handler.addProcessListener(object : CapturingProcessAdapter() {
                        override fun processTerminated(event: ProcessEvent) {
                            try {
                                if (event.exitCode == 0) {
                                    val formattedText = try {
                                        readFormattedText(inputFile, request.documentText)
                                    } catch (e: Exception) {
                                        LOG.warn("Failed to read formatted file `${inputFile.file.absolutePath}`", e)
                                        request.onError(
                                            ActonBundle.message("acton.fmt.error.title"),
                                            ActonBundle.message("acton.fmt.error.formatted.file.read"),
                                        )
                                        return
                                    }
                                    request.onTextReady(formattedText)
                                    return
                                }

                                reportError(request)
                            } finally {
                                deleteTempFile(inputFile.file)
                            }
                        }
                    })
                    handler.startNotify()
                }

                override fun cancel(): Boolean {
                    handler.destroyProcess()
                    return true
                }

                override fun isRunUnderProgress(): Boolean = true
            }
        } catch (e: ExecutionException) {
            deleteTempFile(inputFile.file)
            LOG.warn("Failed to start acton fmt", e)
            request.onError(
                ActonBundle.message("acton.fmt.error.title"),
                ActonBundle.message("acton.fmt.error.execution", e.message ?: "Unknown error"),
            )
            null
        }
    }

    private fun createCommandLine(request: AsyncFormattingRequest, file: File) = ActonCommandLine.forProject(
        project = request.context.project,
        command = "fmt",
        workingDirectory = request.context.project.basePath?.let(Paths::get)
            ?: file.parentFile?.toPath()
            ?: Paths.get("."),
        additionalArguments = buildList {
            add("--color")
            add("never")
            request.context.project.basePath?.let { projectRoot ->
                add("--project-root")
                add(projectRoot)
            }
            add(file.absolutePath)
        },
    ).toGeneralCommandLine(request.context.project)

    private fun createFormattingInputFile(request: AsyncFormattingRequest): FormattingInputFile? {
        val charset = request.context.virtualFile?.charset ?: Charset.defaultCharset()
        return try {
            val file = Files.createTempFile("ij-acton-fmt-", ".tolk").toFile()
            file.writeText(request.documentText, charset)
            FormattingInputFile(file, charset)
        } catch (e: Exception) {
            LOG.warn("Failed to create temporary file for acton fmt", e)
            request.onError(
                ActonBundle.message("acton.fmt.error.title"),
                ActonBundle.message("acton.fmt.error.file.unavailable"),
            )
            null
        }
    }

    private fun readFormattedText(inputFile: FormattingInputFile, originalText: String): String? {
        val formattedText = inputFile.file.readText(inputFile.charset)
        return if (formattedText == originalText) null else formattedText
    }

    private fun deleteTempFile(file: File) {
        if (!file.delete()) {
            file.deleteOnExit()
        }
    }

    private fun reportError(request: AsyncFormattingRequest) {
        request.onError(
            ActonBundle.message("acton.fmt.error.title"),
            ActonBundle.message("acton.fmt.error.syntax.in.file"),
        )
    }

    private fun isActonAvailable(file: PsiFile): Boolean {
        val configuredPath = file.project.actonSettings.actonPath?.trim().orEmpty()
        if (configuredPath.isNotEmpty()) {
            return isExecutable(configuredPath)
        }
        return PathEnvironmentVariableUtil.findInPath("acton") != null
    }

    private fun isExecutable(pathOrCommand: String): Boolean {
        PathEnvironmentVariableUtil.findInPath(pathOrCommand)?.let { return true }
        return try {
            val file = Paths.get(pathOrCommand).toFile()
            file.isFile && file.canExecute()
        } catch (_: Exception) {
            false
        }
    }

    private data class FormattingInputFile(val file: File, val charset: Charset)

    companion object {
        private val LOG = logger<ActonFmtFormattingService>()

        private enum class FormattingReason {
            ReformatCode,
            ReformatCodeBeforeCommit,
            Implicit,
        }

        private fun getFormattingReason(): FormattingReason = when (CommandProcessor.getInstance().currentCommandName) {
            ReformatCodeProcessor.getCommandName() -> FormattingReason.ReformatCode
            FormatterUtil.getReformatBeforeCommitCommandName() -> FormattingReason.ReformatCodeBeforeCommit
            else -> FormattingReason.Implicit
        }
    }
}
