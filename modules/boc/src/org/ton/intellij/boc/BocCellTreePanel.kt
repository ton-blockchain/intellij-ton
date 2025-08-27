package org.ton.intellij.boc

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JPanel

class BocCellTreePanel(
    private val project: Project,
    private val file: VirtualFile,
) : JPanel(BorderLayout()) {

    private val document: Document = EditorFactory.getInstance().createDocument("")
    private val editor: Editor = EditorFactory.getInstance().createViewer(document, project)

    init {
        setupUI()
        loadContent()
    }

    private fun setupUI() {
        add(editor.component, BorderLayout.CENTER)
    }

    private fun loadContent() {
        if (!TasmService.isAvailable()) {
            showInstallPrompt()
            return
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            val result = TasmService.showCellTree(file.path)

            ApplicationManager.getApplication().invokeLater {
                result.onSuccess { output ->
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

    private fun showInstallPrompt() {
        removeAll()

        val installPanel = InstallTonAssemblyActionPanel(project) { onTasmInstalled() }

        val padded = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(12, 12, 0, 0)
            add(installPanel, BorderLayout.NORTH)
        }

        add(padded, BorderLayout.CENTER)
        revalidate()
        repaint()
    }

    private fun onTasmInstalled() {
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