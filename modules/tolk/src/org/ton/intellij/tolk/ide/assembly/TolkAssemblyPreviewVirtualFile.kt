package org.ton.intellij.tolk.ide.assembly

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import org.ton.intellij.tasm.TasmFileType
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

class TolkAssemblyPreviewVirtualFile(val sourceFile: VirtualFile) :
    LightVirtualFile("${sourceFile.name} [Assembly]", PlainTextFileType.INSTANCE, "") {
    val assemblyFile = LightVirtualFile(
        "${sourceFile.nameWithoutExtension}.tasm",
        TasmFileType,
        LOADING_TEXT,
    )

    @Volatile
    var presentation: TolkAssemblyPreviewPresentation = TolkAssemblyPreviewPresentation.loading()
        private set

    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    private val refreshSequence = AtomicLong()

    init {
        setOriginalFile(sourceFile)
        setWritable(false)
    }

    fun startRefresh(): Long {
        val refreshId = refreshSequence.incrementAndGet()
        presentation = TolkAssemblyPreviewPresentation.loading()
        updateAssemblyText(LOADING_TEXT)
        notifyListeners()
        return refreshId
    }

    fun completeRefresh(
        refreshId: Long,
        status: TolkAssemblyPreviewStatus,
        assemblyText: String,
        blocks: List<TolkAssemblyPreviewBlock> = emptyList(),
    ) {
        if (refreshId != refreshSequence.get()) {
            return
        }

        presentation = TolkAssemblyPreviewPresentation(status, blocks)
        updateAssemblyText(assemblyText)
        notifyListeners()
    }

    fun addListener(parentDisposable: Disposable, listener: () -> Unit) {
        listeners.add(listener)
        Disposer.register(parentDisposable) {
            listeners.remove(listener)
        }
    }

    private fun updateAssemblyText(assemblyText: String) {
        val document = FileDocumentManager.getInstance().getDocument(assemblyFile)
        if (document != null) {
            document.setText(assemblyText)
        } else {
            assemblyFile.setContent(this, assemblyText, false)
        }
    }

    private fun notifyListeners() {
        listeners.forEach { it() }
    }

    private companion object {
        const val LOADING_TEXT = "// Building assembly preview..."
    }
}
