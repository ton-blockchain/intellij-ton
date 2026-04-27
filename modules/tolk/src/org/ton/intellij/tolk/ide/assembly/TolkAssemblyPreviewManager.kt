package org.ton.intellij.tolk.ide.assembly

import com.google.gson.Gson
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessAdapter
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessNotCreatedException
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import org.ton.intellij.acton.cli.ActonCommand
import org.ton.intellij.acton.cli.ActonCommandLine
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object TolkAssemblyPreviewManager {
    private val log = logger<TolkAssemblyPreviewManager>()
    private val gson = Gson()

    fun open(project: Project, sourceFile: VirtualFile) {
        val previewFile = findPreview(project, sourceFile) ?: TolkAssemblyPreviewVirtualFile(sourceFile)
        FileEditorManager.getInstance(project).openFile(previewFile, true)
        refresh(project, previewFile)
    }

    fun refresh(project: Project, previewFile: TolkAssemblyPreviewVirtualFile) {
        saveSourceDocument(previewFile.sourceFile)

        val refreshId = runWriteAction {
            previewFile.startRefresh()
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project,
            "Compiling ${previewFile.sourceFile.name}",
        ) {
            override fun run(indicator: ProgressIndicator) {
                val result = compileAndDisassemble(project, previewFile.sourceFile)
                ApplicationManager.getApplication().invokeLater {
                    if (project.isDisposed) {
                        return@invokeLater
                    }

                    runWriteAction {
                        if (!previewFile.isValid) {
                            return@runWriteAction
                        }

                        if (result.isSuccess) {
                            val output = result.getOrThrow()
                            previewFile.completeRefresh(
                                refreshId = refreshId,
                                status = TolkAssemblyPreviewStatus.Ready,
                                assemblyText = output.assemblyText,
                                blocks = output.blocks,
                            )
                        } else {
                            val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                            val cleanedMessage = stripAnsiCodes(errorMessage).trim()
                            previewFile.completeRefresh(
                                refreshId = refreshId,
                                status = TolkAssemblyPreviewStatus.Failed(cleanedMessage),
                                assemblyText = formatFailureText(cleanedMessage),
                            )
                        }
                    }
                }
            }
        })
    }

    private fun findPreview(project: Project, sourceFile: VirtualFile): TolkAssemblyPreviewVirtualFile? =
        FileEditorManager.getInstance(project).openFiles
            .filterIsInstance<TolkAssemblyPreviewVirtualFile>()
            .firstOrNull { it.sourceFile.url == sourceFile.url }

    private fun saveSourceDocument(sourceFile: VirtualFile) {
        val document = FileDocumentManager.getInstance().getDocument(sourceFile) ?: return
        FileDocumentManager.getInstance().saveDocument(document)
    }

    private fun compileAndDisassemble(project: Project, sourceFile: VirtualFile): Result<TolkAssemblyPreviewOutput> {
        val workingDir = project.guessProjectDir()?.toNioPath()
            ?: return Result.failure(IllegalStateException("Cannot determine project directory"))
        val sourceMapPath = try {
            Files.createTempFile("tolk-assembly-preview-", ".source-map.json")
        } catch (e: Exception) {
            return Result.failure(IllegalStateException("Cannot create temporary source map file", e))
        }

        try {
            val compileCommand = ActonCommand.Compile(
                path = sourceFile.path,
                json = true,
                sourceMap = sourceMapPath.toString(),
                allowNoEntrypoint = true,
            )
            val compileCommandLine = ActonCommandLine(
                command = compileCommand.name,
                workingDirectory = workingDir,
                additionalArguments = compileCommand.getArguments(),
            ).toGeneralCommandLine(project)
                ?: return Result.failure(IllegalStateException("Cannot find acton executable"))

            val compileJson = runExternal(compileCommandLine)
                .getOrElse {
                    return Result.failure(
                        IllegalStateException("Failed to compile file '${sourceFile.path}': ${it.message}"),
                    )
                }
            val compileResult = parseCompileResult(compileJson)
                .getOrElse { return Result.failure(it) }
            val base64Code = compileResult.code_boc64
                ?: return Result.failure(IllegalStateException("Compilation JSON did not include code_boc64"))

            val disasmCommand = ActonCommand.Disasm(
                string = base64Code,
                json = true,
                sourceMap = sourceMapPath.toString(),
            )
            val disasmCommandLine = ActonCommandLine(
                command = disasmCommand.name,
                workingDirectory = workingDir,
                additionalArguments = disasmCommand.getArguments(),
            ).toGeneralCommandLine(project)
                ?: return Result.failure(IllegalStateException("Cannot find acton executable"))

            val disasmJson = runExternal(disasmCommandLine)
                .getOrElse {
                    return Result.failure(IllegalStateException("Failed to disassemble compiled code: ${it.message}"))
                }
            val disasmResult = parseDisasmResult(disasmJson, sourceFile)
                .getOrElse { return Result.failure(it) }

            return Result.success(disasmResult)
        } finally {
            try {
                Files.deleteIfExists(sourceMapPath)
            } catch (_: Exception) {
            }
        }
    }

    private fun parseCompileResult(commandOutput: String): Result<TolkCompileJsonResult> {
        val compileResult = try {
            gson.fromJson(commandOutput, TolkCompileJsonResult::class.java)
        } catch (e: Exception) {
            return Result.failure(IllegalStateException("Failed to parse compile JSON: ${e.message}", e))
        }
        if (!compileResult.success) {
            return Result.failure(IllegalStateException(compileResult.error ?: "Compilation failed"))
        }
        if (compileResult.code_boc64.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Compilation succeeded but produced empty code_boc64"))
        }
        return Result.success(compileResult)
    }

    private fun parseDisasmResult(commandOutput: String, sourceFile: VirtualFile): Result<TolkAssemblyPreviewOutput> {
        val disasmResult = try {
            gson.fromJson(commandOutput, TolkDisasmJsonResult::class.java)
        } catch (e: Exception) {
            return Result.failure(IllegalStateException("Failed to parse disasm JSON: ${e.message}", e))
        }
        if (!disasmResult.success) {
            return Result.failure(IllegalStateException(disasmResult.error ?: "Disassembly failed"))
        }
        val assemblyText = disasmResult.assembly
            ?: return Result.failure(IllegalStateException("Disassembly JSON did not include assembly text"))
        return Result.success(
            TolkAssemblyPreviewOutput(
                assemblyText = assemblyText,
                blocks = buildPreviewBlocks(disasmResult.blocks, sourceFile),
            ),
        )
    }

    private fun buildPreviewBlocks(
        jsonBlocks: List<TolkDisasmJsonBlock>,
        sourceFile: VirtualFile,
    ): List<TolkAssemblyPreviewBlock> {
        val sourcePath = normalizePath(sourceFile.path)
        val lineToAssemblyRanges = linkedMapOf<Int, MutableList<IntRange>>()

        jsonBlocks.forEach { block ->
            val source = block.source ?: return@forEach
            if (normalizePath(source.file) != sourcePath) {
                return@forEach
            }
            val sourceLine = source.line - 1
            if (sourceLine < 0) {
                return@forEach
            }
            val assemblyRanges = block.assembly_ranges.mapNotNull { range ->
                if (range.end_line < range.start_line || range.start_line < 0) {
                    return@mapNotNull null
                }
                range.start_line..range.end_line
            }
            if (assemblyRanges.isEmpty()) {
                return@forEach
            }

            lineToAssemblyRanges.getOrPut(sourceLine) { mutableListOf() }.addAll(assemblyRanges)
        }

        return lineToAssemblyRanges.entries
            .sortedBy { it.key }
            .map { (sourceLine, assemblyRanges) ->
                TolkAssemblyPreviewBlock(
                    sourceLines = sourceLine..sourceLine,
                    assemblyLines = mergeAssemblyRanges(assemblyRanges),
                )
            }
    }

    private fun mergeAssemblyRanges(ranges: List<IntRange>): List<IntRange> {
        if (ranges.isEmpty()) {
            return emptyList()
        }

        val sortedRanges = ranges.sortedWith(compareBy<IntRange>({ it.first }, { it.last }))
        val merged = mutableListOf<IntRange>()

        sortedRanges.forEach { range ->
            val lastRange = merged.lastOrNull()
            if (lastRange == null || range.first > lastRange.last + 1) {
                merged += range
            } else {
                merged[merged.lastIndex] = lastRange.first..maxOf(lastRange.last, range.last)
            }
        }

        return merged
    }

    private fun normalizePath(path: String): String = try {
        Paths.get(path).normalize().toAbsolutePath().toString()
    } catch (_: Exception) {
        path
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
                    stderrTrimmed.isNotEmpty() -> stderrTrimmed
                    stdoutTrimmed.isNotEmpty() -> stdoutTrimmed
                    else -> "Exit code $exitCode"
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

    private fun formatFailureText(message: String): String = message.lines().joinToString("\n") { "// $it" }
}
