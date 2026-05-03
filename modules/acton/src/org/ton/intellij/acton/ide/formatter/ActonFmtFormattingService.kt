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
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.formatter.FormatterUtil
import org.ton.intellij.acton.ActonBundle
import org.ton.intellij.acton.cli.ActonCommandLine
import org.ton.intellij.acton.cli.findActonExecutable
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

    override fun getFeatures(): MutableSet<Feature> = mutableSetOf(Feature.FORMAT_FRAGMENTS)

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
            createActonFmtRangeArgument(request.documentText, request.formattingRanges)?.let { range ->
                add("--range")
                add(range)
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
        return findActonExecutable() != null
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

        internal fun createActonFmtRangeArgument(documentText: String, ranges: List<TextRange>): String? {
            val normalizedRanges = ranges
                .mapNotNull { range ->
                    val start = range.startOffset.coerceIn(0, documentText.length)
                    val end = range.endOffset.coerceIn(0, documentText.length)
                    if (start >= end) null else TextRange(start, end)
                }

            if (normalizedRanges.isEmpty()) return null

            val startOffset = normalizedRanges.minOf { it.startOffset }
            val endOffset = normalizedRanges.maxOf { it.endOffset }
            if (startOffset == 0 && endOffset == documentText.length) return null

            val start = documentText.toActonFmtPosition(startOffset)
            val end = documentText.toActonFmtPosition(endOffset)
            return "${start.line}:${start.byteColumn}-${end.line}:${end.byteColumn}"
        }

        private fun String.toActonFmtPosition(offset: Int): ActonFmtPosition {
            val normalizedOffset = offset.coerceIn(0, length)
            var line = 0
            var lineStartOffset = 0

            for (index in 0 until normalizedOffset) {
                if (this[index] == '\n') {
                    line++
                    lineStartOffset = index + 1
                }
            }

            val byteColumn = substring(lineStartOffset, normalizedOffset).toByteArray(Charsets.UTF_8).size
            return ActonFmtPosition(line, byteColumn)
        }

        internal data class ActonFmtPosition(val line: Int, val byteColumn: Int)

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
