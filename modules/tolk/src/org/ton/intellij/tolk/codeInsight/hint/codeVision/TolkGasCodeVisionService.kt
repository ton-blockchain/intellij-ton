package org.ton.intellij.tolk.codeInsight.hint.codeVision

import com.google.gson.Gson
import com.intellij.codeInsight.codeVision.CodeVisionHost
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.Alarm
import com.intellij.util.concurrency.AppExecutorUtil
import org.ton.intellij.acton.cli.ActonCommand
import org.ton.intellij.acton.cli.ActonCommandLine
import org.ton.intellij.acton.settings.actonSettings
import org.ton.intellij.tolk.ide.assembly.TolkDisasmJsonFunction
import org.ton.intellij.tolk.ide.assembly.TolkDisasmJsonResult
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Keeps one cached batch per source file. Source/config edits invalidate imported dependencies too.
 * Requests are debounced, run serially outside read actions, and are cancelled on edits or project disposal.
 * Failures are cached until inputs change so an old CLI or a compile error cannot cause a retry loop.
 */
@Service(Service.Level.PROJECT)
internal class TolkGasCodeVisionService(private val project: Project) : Disposable {
    private val executor = AppExecutorUtil.createBoundedScheduledExecutorService("Tolk gas estimates", 1)
    private val refreshAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)
    private val generation = AtomicLong()
    private val requests = ConcurrentHashMap<String, Request>()

    init {
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(
            object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    val file = FileDocumentManager.getInstance().getFile(event.document) ?: return
                    if (isInput(file.path)) invalidate()
                }
            },
            this,
        )
        ApplicationManager.getApplication().messageBus.connect(this).subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    if (events.any { it.file?.isDirectory == true || isInput(it.path) }) invalidate()
                }
            },
        )
    }

    fun request(source: VirtualFile, workingDirectory: Path, names: List<String>): Map<String, TolkDisasmJsonFunction> {
        val key =
            RequestKey(generation.get(), source.modificationStamp, project.actonSettings.stateModificationCount, names)
        val request = requests.compute(source.path) { _, previous ->
            if (previous?.key == key) {
                previous
            } else {
                previous?.cancel()
                Request(key).also { next ->
                    next.future =
                        executor.schedule({
                            calculate(source.path, workingDirectory, next)
                        }, 400, TimeUnit.MILLISECONDS)
                }
            }
        } ?: return emptyMap()
        return request.result
    }

    private fun calculate(path: String, workingDirectory: Path, request: Request) {
        val started = System.nanoTime()
        LOG.debug("operation=gas_estimate target=$path functions=${request.key.names.size} outcome=started")
        var outcome = "failed"
        try {
            if (Thread.currentThread().isInterrupted || project.isDisposed) return
            val command = ActonCommand.Disasm(bocFile = path, json = true, functions = request.key.names)
            val commandLine = ActonCommandLine(command.name, workingDirectory, command.getArguments())
                .toGeneralCommandLine(project) ?: return
            val handler = CapturingProcessHandler(commandLine)
            request.process = handler
            if (Thread.currentThread().isInterrupted || project.isDisposed) return
            val output = handler.runProcess(30_000)
            if (output.isTimeout || output.exitCode != 0) return
            val result = Gson().fromJson(output.stdout, TolkDisasmJsonResult::class.java) ?: return
            if (!result.success) return
            if (requests[path] !== request || request.key.generation != generation.get()) return
            request.result = result.functions.associateBy { it.name }
            outcome = "success"
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Exception) {
            LOG.debug("operation=gas_estimate target=$path outcome=failed", e)
        } finally {
            request.process?.let { if (!it.isProcessTerminated) it.destroyProcess() }
            request.process = null
            val duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
            LOG.debug("operation=gas_estimate target=$path duration_ms=$duration outcome=$outcome")
            if (requests[path] === request && !project.isDisposed) refresh()
        }
    }

    private fun invalidate() {
        generation.incrementAndGet()
        requests.values.forEach { it.cancel() }
        requests.clear()
        refresh()
    }

    private fun refresh() {
        ApplicationManager.getApplication().invokeLater {
            if (!project.isDisposed) {
                refreshAlarm.cancelAllRequests()
                refreshAlarm.addRequest({
                    if (!project.isDisposed) {
                        project.service<CodeVisionHost>().invalidateProvider(
                            CodeVisionHost.LensInvalidateSignal(null, listOf(TolkGasCodeVisionProvider.ID)),
                        )
                    }
                }, 200)
            }
        }
    }

    override fun dispose() {
        requests.values.forEach { it.cancel() }
        requests.clear()
        executor.shutdownNow()
    }

    private data class RequestKey(
        val generation: Long,
        val sourceStamp: Long,
        val settingsStamp: Long,
        val names: List<String>,
    )

    private class Request(val key: RequestKey) {
        @Volatile var result: Map<String, TolkDisasmJsonFunction> = emptyMap()

        @Volatile var process: CapturingProcessHandler? = null
        var future: Future<*>? = null

        fun cancel() {
            future?.cancel(true)
            process?.destroyProcess()
        }
    }

    companion object {
        private val LOG = logger<TolkGasCodeVisionService>()

        private fun isInput(path: String): Boolean = path.endsWith(".tolk") || path.endsWith("/Acton.toml")
    }
}
