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
    private val detailedDisassemblyPanel = BocDetailedDisassemblyPanel(project, file) { onInstalled() }
    private val hexPanel = BocHexPanel(project, file) { onInstalled() }
    private val base64Panel = BocBase64Panel(project, file) { onInstalled() }

    private val tabs: JBTabs = JBTabsFactory.createEditorTabs(project, this).apply {
        presentation.setInnerInsets(JBInsets.emptyInsets())
        presentation.setTabsPosition(JBTabsPosition.bottom)
    }

    private val tasmTab = TabInfo(disassemblyPanel).setText("TASM")
    private val detailedTab = TabInfo(detailedDisassemblyPanel).setText("Detailed TASM")
    private val hexTab = TabInfo(hexPanel).setText("Hex")
    private val base64Tab = TabInfo(base64Panel).setText("Base64")

    init {
        tabs.addTab(tasmTab)
        tabs.addTab(detailedTab)
        tabs.addTab(hexTab)
        tabs.addTab(base64Tab)
        tabs.select(tasmTab, true)
    }

    private fun onInstalled() {
        disassemblyPanel.onTasmInstalled()
        detailedDisassemblyPanel.onTasmInstalled()
        hexPanel.onTasmInstalled()
        base64Panel.onTasmInstalled()
    }

    override fun getComponent(): JComponent = tabs.component
    override fun getPreferredFocusedComponent(): JComponent = tabs.component
    override fun getName(): String = "BoC Viewer"
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
        detailedDisassemblyPanel.dispose()
        super.dispose()
    }
}
