package org.ton.intellij.boc

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JPanel

abstract class BocBasePanel(
    protected val project: Project,
    protected val file: VirtualFile,
    fileType: LanguageFileType = PlainTextFileType.INSTANCE,
    private val onInstalled: (() -> Unit)? = null,
) : JPanel(BorderLayout()), Disposable {

    protected val document: Document = EditorFactory.getInstance().createDocument("")
    protected val editor: Editor = EditorFactory.getInstance().createEditor(document, project, fileType, true)

    init {
        setupUI()
        loadContent()
        setupFileListener()
    }

    private fun setupFileListener() {
        val connection = project.messageBus.connect(this)
        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                for (event in events) {
                    if (event is VFileContentChangeEvent && event.file == file) {
                        loadContent()
                        break
                    }
                }
            }
        })
    }

    private fun setupUI() {
        add(editor.component)
    }

    private fun loadContent() {
        if (!BocActonService.isAvailable(project)) {
            showInstallPrompt()
            return
        }

        file.refresh(false, false)

        ApplicationManager.getApplication().executeOnPooledThread {
            val result = runTask()

            ApplicationManager.getApplication().invokeLater {
                if (project.isDisposed) return@invokeLater
                result
                    .onSuccess { output ->
                        ApplicationManager.getApplication().runWriteAction {
                            document.setText(output)
                        }
                    }.onFailure { error ->
                        ApplicationManager.getApplication().runWriteAction {
                            document.setText("Error: ${error.message}")
                        }
                    }
            }
        }
    }

    protected abstract fun runTask(): Result<String>

    private fun showInstallPrompt() {
        removeAll()

        val installPanel = SetupActonActionPanel(project, onInstalled)

        val padded = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(12, 12, 0, 0)
            add(installPanel, BorderLayout.NORTH)
        }

        add(padded, BorderLayout.CENTER)
        revalidate()
        repaint()
    }

    fun onTasmInstalled() {
        removeAll()
        add(editor.component, BorderLayout.CENTER)
        revalidate()
        repaint()
        loadContent()
    }

    override fun dispose() {
        EditorFactory.getInstance().releaseEditor(editor)
    }
}