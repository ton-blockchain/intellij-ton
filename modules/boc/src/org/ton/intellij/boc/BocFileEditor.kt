@file:Suppress("UnstableApiUsage")

package org.ton.intellij.boc

import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.diff.util.FileEditorBase
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.tabs.JBTabs
import com.intellij.ui.tabs.JBTabsFactory
import com.intellij.ui.tabs.TabInfo
import com.intellij.ui.tabs.JBTabsPosition
import com.intellij.util.ui.JBInsets
import java.beans.PropertyChangeListener
import javax.swing.JComponent

class BocFileEditor(
    project: Project,
    private val file: VirtualFile,
) : FileEditorBase() {

    private val disassemblyPanel = BocDisassemblyPanel(project, file) { onInstalled() }
    private val cellTreePanel = BocCellTreePanel(project, file) { onInstalled() }

    private val tabs: JBTabs = JBTabsFactory.createEditorTabs(project, this).apply {
        presentation.setInnerInsets(JBInsets.emptyInsets())
        presentation.setTabsPosition(JBTabsPosition.bottom)
    }

    private val tasmTab = TabInfo(disassemblyPanel).setText("TASM")
    private val cellTab = TabInfo(cellTreePanel).setText("Cell Tree")

    init {
        tabs.addTab(tasmTab)
        tabs.addTab(cellTab)
        tabs.select(tasmTab, true)
    }

    private fun onInstalled() {
        disassemblyPanel.onTasmInstalled()
        cellTreePanel.onTasmInstalled()
    }

    override fun getComponent(): JComponent = tabs.component
    override fun getPreferredFocusedComponent(): JComponent = tabs.component
    override fun getName(): String = "BOC Viewer"
    override fun setState(state: FileEditorState) {}
    override fun isModified(): Boolean = false
    override fun isValid(): Boolean = file.isValid
    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}
    override fun getCurrentLocation(): FileEditorLocation? = null
    override fun getBackgroundHighlighter(): BackgroundEditorHighlighter? = null
    override fun getFile(): VirtualFile = file

    override fun dispose() {
        disassemblyPanel.dispose()
        cellTreePanel.dispose()
        super.dispose()
    }
}
