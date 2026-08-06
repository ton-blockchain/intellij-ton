package org.ton.intellij.acton.ide

import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotifications
import org.ton.intellij.acton.cli.ActonCommandLine
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

val Project.actonWrapperFreshness: ActonWrapperFreshnessService
    get() = service()

internal enum class ActonWrapperFreshness {
    CHECKING,
    UP_TO_DATE,
    OUTDATED,
    UNKNOWN,
}

@Service(Service.Level.PROJECT)
class ActonWrapperFreshnessService(private val project: Project) {
    private val entries = ConcurrentHashMap<String, CacheEntry>()
    private val updates = ConcurrentHashMap.newKeySet<String>()

    internal fun check(target: ActonWrapperTarget): ActonWrapperFreshness {
        val key = CacheKey.from(target)
        val path = target.wrapperFile.path
        val current = entries[path]
        if (current?.key == key) return current.state

        val entry = CacheEntry(key, ActonWrapperFreshness.CHECKING)
        entries[path] = entry
        ApplicationManager.getApplication().executeOnPooledThread {
            val state = checkInBackground(target)
            if (project.isDisposed) return@executeOnPooledThread

            ApplicationManager.getApplication().invokeLater {
                if (project.isDisposed) return@invokeLater
                if (entries[path]?.key != key) return@invokeLater

                entries[path] = CacheEntry(key, state)
                EditorNotifications.getInstance(project).updateNotifications(target.wrapperFile)
            }
        }

        return ActonWrapperFreshness.CHECKING
    }

    internal fun update(target: ActonWrapperTarget) {
        val path = target.wrapperFile.path
        if (!updates.add(path)) return

        val key = CacheKey.from(target)
        entries[path] = CacheEntry(key, ActonWrapperFreshness.CHECKING)
        ApplicationManager.getApplication().executeOnPooledThread {
            val result = runWrapper(target, target.wrapperFile.path)
            updates.remove(path)

            if (project.isDisposed) return@executeOnPooledThread
            ApplicationManager.getApplication().invokeLater {
                if (project.isDisposed) return@invokeLater

                if (result.success) {
                    runWriteAction {
                        target.wrapperFile.refresh(false, false)
                        FileDocumentManager.getInstance().getDocument(target.wrapperFile)?.let { document ->
                            FileDocumentManager.getInstance().reloadFromDisk(document)
                        }
                    }
                    entries.remove(path)
                    EditorNotifications.getInstance(project).updateNotifications(target.wrapperFile)
                } else {
                    entries[path] = CacheEntry(key, ActonWrapperFreshness.OUTDATED)
                    EditorNotifications.getInstance(project).updateNotifications(target.wrapperFile)
                }
            }
        }
    }

    private fun checkInBackground(target: ActonWrapperTarget): ActonWrapperFreshness {
        val parent = target.wrapperFile.parent ?: return ActonWrapperFreshness.UNKNOWN
        val parentPath = runCatching { Path.of(parent.path) }.getOrNull() ?: return ActonWrapperFreshness.UNKNOWN
        val temporaryFile = runCatching {
            Files.createTempFile(parentPath, ".acton-wrapper-", target.language.fileSuffix)
        }.getOrNull() ?: return ActonWrapperFreshness.UNKNOWN

        return try {
            val result = runWrapper(target, temporaryFile.toString())
            if (!result.success) return ActonWrapperFreshness.UNKNOWN

            val generated = Files.readString(temporaryFile)
            val current = readWrapper(target.wrapperFile) ?: return ActonWrapperFreshness.UNKNOWN
            if (normalize(generated) == normalize(current)) {
                ActonWrapperFreshness.UP_TO_DATE
            } else {
                ActonWrapperFreshness.OUTDATED
            }
        } catch (_: Exception) {
            ActonWrapperFreshness.UNKNOWN
        } finally {
            runCatching { Files.deleteIfExists(temporaryFile) }
        }
    }

    private fun runWrapper(target: ActonWrapperTarget, outputPath: String): WrapperProcessResult {
        val arguments = buildList {
            add(target.contractId)
            if (target.language == ActonWrapperLanguage.TYPESCRIPT) add("--ts")
            add("--output")
            add(outputPath)
        }

        val commandLine = ActonCommandLine(
            command = "wrapper",
            workingDirectory = target.actonToml.workingDir,
            additionalArguments = arguments,
        ).toGeneralCommandLine(project) ?: return WrapperProcessResult(false)

        return try {
            val output = CapturingProcessHandler(commandLine).runProcess(CHECK_TIMEOUT_MS)
            WrapperProcessResult(output.exitCode == 0)
        } catch (_: Exception) {
            WrapperProcessResult(false)
        }
    }

    private fun readWrapper(file: VirtualFile): String? = try {
        runReadAction {
            FileDocumentManager.getInstance().getDocument(file)?.text
                ?: String(file.contentsToByteArray(), Charsets.UTF_8)
        }
    } catch (_: Exception) {
        null
    }

    private fun normalize(content: String): String = content.replace("\r\n", "\n").replace('\r', '\n')

    private data class CacheEntry(val key: CacheKey, val state: ActonWrapperFreshness)

    private data class CacheKey(
        val contractId: String,
        val language: ActonWrapperLanguage,
        val wrapperStamp: Long,
        val documentStamp: Long,
        val actonTomlStamp: Long,
        val sourceStamp: Long,
        val typesStamp: Long,
    ) {
        companion object {
            fun from(target: ActonWrapperTarget): CacheKey {
                val documentStamp = runCatching {
                    runReadAction {
                        FileDocumentManager.getInstance()
                            .getDocument(target.wrapperFile)
                            ?.modificationStamp
                            ?: -1L
                    }
                }.getOrDefault(-1L)
                return CacheKey(
                    contractId = target.contractId,
                    language = target.language,
                    wrapperStamp = target.wrapperFile.modificationStamp,
                    documentStamp = documentStamp,
                    actonTomlStamp = target.actonToml.virtualFile.modificationStamp,
                    sourceStamp = target.sourceFile.modificationStamp,
                    typesStamp = target.typesFile?.modificationStamp ?: -1L,
                )
            }
        }
    }

    private data class WrapperProcessResult(val success: Boolean)

    private companion object {
        private const val CHECK_TIMEOUT_MS = 120_000
    }
}
