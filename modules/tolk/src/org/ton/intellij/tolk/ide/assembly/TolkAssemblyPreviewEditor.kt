package org.ton.intellij.tolk.ide.assembly

import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.diff.tools.util.DiffSplitter
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBSplitter

class TolkAssemblyPreviewEditor(
    private val project: Project,
    internal val previewFile: TolkAssemblyPreviewVirtualFile,
) : TextEditorWithPreview(
    createTextEditor(project, previewFile.sourceFile),
    createTextEditor(project, previewFile.assemblyFile),
    "Assembly Preview",
) {
    internal val sourceTextEditor: TextEditor
        get() = textEditor

    internal val assemblyTextEditor: TextEditor
        get() = previewEditor as TextEditor

    private var splitter: DiffSplitter? = null
    private val blockHighlighters = mutableListOf<RangeHighlighter>()

    init {
        setVerticalSplit(false)
        (assemblyTextEditor.editor as? EditorEx)?.setViewer(true)
        previewFile.addListener(this) {
            rebuildHighlights()
            splitter?.repaintDivider()
        }
        rebuildHighlights()
    }

    override fun createSplitter(): JBSplitter = DiffSplitter().also { diffSplitter ->
        splitter = diffSplitter
        diffSplitter.setPainter(TolkAssemblyDividerPainter(this))
    }

    override fun createRightToolbarActionGroup(): ActionGroup = DefaultActionGroup().apply {
        add(RefreshAssemblyAction())
    }

    override fun getBackgroundHighlighter(): BackgroundEditorHighlighter? = null

    override fun getFile(): VirtualFile = previewFile

    override fun dispose() {
        clearHighlights()
        super.dispose()
    }

    private inner class RefreshAssemblyAction :
        DumbAwareAction(
            "Refresh Assembly",
            "Recompile the source file and rebuild the assembly preview",
            AllIcons.Actions.Refresh,
        ) {
        override fun actionPerformed(e: AnActionEvent) {
            TolkAssemblyPreviewManager.refresh(project, previewFile)
        }

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = previewFile.presentation.status != TolkAssemblyPreviewStatus.Loading
        }
    }

    private companion object {
        private fun createTextEditor(project: Project, file: VirtualFile): TextEditor =
            TextEditorProvider.getInstance().createEditor(project, file) as TextEditor
    }

    private fun rebuildHighlights() {
        clearHighlights()

        val sourceEditor = sourceTextEditor.editor
        val assemblyEditor = assemblyTextEditor.editor

        previewFile.presentation.blocks.forEachIndexed { index, block ->
            val colors = TolkAssemblyBlockPalette.colorsFor(index)
            val sourceAttributes = TextAttributes().apply { backgroundColor = colors.sourceBackground }
            val assemblyAttributes = TextAttributes().apply { backgroundColor = colors.assemblyBackground }
            addLineHighlighter(sourceEditor.document, sourceEditor.markupModel, block.sourceLines, sourceAttributes)
            block.assemblyLines.forEach { assemblyLines ->
                addLineHighlighter(
                    assemblyEditor.document,
                    assemblyEditor.markupModel,
                    assemblyLines,
                    assemblyAttributes,
                )
            }
        }
    }

    private fun addLineHighlighter(
        document: Document,
        markupModel: com.intellij.openapi.editor.markup.MarkupModel,
        lines: IntRange,
        attributes: TextAttributes,
    ) {
        val lineCount = document.lineCount
        if (lineCount <= 0) {
            return
        }

        val startLine = lines.first.coerceIn(0, lineCount - 1)
        val endLine = lines.last.coerceIn(startLine, lineCount - 1)
        val startOffset = document.getLineStartOffset(startLine)
        val endOffset = document.getLineEndOffset(endLine)
        blockHighlighters += markupModel.addRangeHighlighter(
            startOffset,
            endOffset,
            HighlighterLayer.SELECTION - 1,
            attributes,
            HighlighterTargetArea.LINES_IN_RANGE,
        )
    }

    private fun clearHighlights() {
        blockHighlighters.forEach { highlighter ->
            if (highlighter.isValid) {
                highlighter.dispose()
            }
        }
        blockHighlighters.clear()
    }
}
