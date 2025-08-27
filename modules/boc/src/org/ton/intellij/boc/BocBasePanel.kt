package org.ton.intellij.boc

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JPanel

abstract class BocBasePanel(
    protected val project: Project,
    protected val file: VirtualFile,
    private val onInstalled: (() -> Unit)? = null,
) : JPanel(BorderLayout()) {

    protected val document: Document = EditorFactory.getInstance().createDocument("")
    protected val editor: Editor = EditorFactory.getInstance().createViewer(document, project, EditorKind.MAIN_EDITOR)

    init {
        setupUI()
        loadContent()
    }

    private fun setupUI() {
        add(editor.component)
    }

    private fun loadContent() {
        if (!TasmService.isAvailable()) {
            showInstallPrompt()
            return
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            val result = runTask()

            ApplicationManager.getApplication().invokeLater {
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

        val installPanel = InstallTonAssemblyActionPanel(project, onInstalled)

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

    fun dispose() {
        EditorFactory.getInstance().releaseEditor(editor)
    }
}