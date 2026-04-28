package org.ton.intellij.tolk.ide.assembly

import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.diff.tools.util.DiffSplitter
import com.intellij.diff.util.DiffDrawUtil
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.editor.event.VisibleAreaListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.EditorGutterFreePainterAreaState
import com.intellij.openapi.editor.markup.ActiveGutterRenderer
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
import java.awt.Component
import java.awt.Graphics
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities

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
    private var activeBlockIndex = -1
    private var mouseOverAssemblyGutter = false
    private var mouseOverDivider = false
    private var mouseOverSourceConnectionZone = false
    private var stickyActiveBlockConnection = false
    private var lastShownAssemblyRangeIndex = 0
    private var previousClickedBlockIndex = -1

    init {
        setVerticalSplit(false)
        (assemblyTextEditor.editor as? EditorEx)?.let { assemblyEditor ->
            assemblyEditor.setViewer(true)
            assemblyEditor.gutterComponentEx.setRightFreePaintersAreaState(EditorGutterFreePainterAreaState.SHOW)
            assemblyEditor.gutterComponentEx.addMouseListener(AssemblyGutterMouseListener())
        }
        val sourceConnectionMouseHandler = SourceConnectionMouseHandler()
        sourceTextEditor.editor.contentComponent.addMouseMotionListener(sourceConnectionMouseHandler)
        sourceTextEditor.editor.contentComponent.addMouseListener(sourceConnectionMouseHandler)
        sourceTextEditor.editor.component.addMouseMotionListener(sourceConnectionMouseHandler)
        sourceTextEditor.editor.component.addMouseListener(sourceConnectionMouseHandler)
        sourceTextEditor.editor.addEditorMouseMotionListener(BlockHoverListener(isSourceEditor = true))
        assemblyTextEditor.editor.addEditorMouseMotionListener(BlockHoverListener(isSourceEditor = false))
        val visibleAreaListener = VisibleAreaListener { repaintConnections() }
        sourceTextEditor.editor.scrollingModel.addVisibleAreaListener(visibleAreaListener)
        assemblyTextEditor.editor.scrollingModel.addVisibleAreaListener(visibleAreaListener)
        previewFile.addListener(this) {
            rebuildHighlights()
            repaintConnections()
        }
        rebuildHighlights()
    }

    override fun createSplitter(): JBSplitter = object : DiffSplitter() {
        private val diffDividerWidth = super.getDividerWidth() + CLION_DIVIDER_EXTRA_WIDTH

        override fun getDividerWidth(): Int = diffDividerWidth
    }.also { diffSplitter ->
        splitter = diffSplitter
        diffSplitter.setPainter(TolkAssemblyDividerPainter(this))
        diffSplitter.divider.addMouseMotionListener(DividerMouseHandler())
        diffSplitter.divider.addMouseListener(DividerMouseHandler())
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

    private fun rebuildHighlights() {
        clearHighlights()
        if (activeBlockIndex >= previewFile.presentation.blocks.size) {
            activeBlockIndex = -1
        }

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
                )?.lineMarkerRenderer = AssemblyGutterRenderer(index, assemblyLines)
            }
        }
    }

    private fun addLineHighlighter(
        document: Document,
        markupModel: com.intellij.openapi.editor.markup.MarkupModel,
        lines: IntRange,
        attributes: TextAttributes,
    ): RangeHighlighter? {
        val lineCount = document.lineCount
        if (lineCount <= 0) {
            return null
        }

        val startLine = lines.first.coerceIn(0, lineCount - 1)
        val endLine = lines.last.coerceIn(startLine, lineCount - 1)
        val startOffset = document.getLineStartOffset(startLine)
        val endOffset = document.getLineEndOffset(endLine)
        return markupModel.addRangeHighlighter(
            startOffset,
            endOffset,
            HighlighterLayer.SELECTION - 1,
            attributes,
            HighlighterTargetArea.LINES_IN_RANGE,
        ).also(blockHighlighters::add)
    }

    private fun clearHighlights() {
        blockHighlighters.forEach { highlighter ->
            if (highlighter.isValid) {
                highlighter.dispose()
            }
        }
        blockHighlighters.clear()
    }

    internal fun activeBlock(): TolkAssemblyPreviewBlock? = previewFile.presentation.blocks.getOrNull(activeBlockIndex)

    internal fun activeBlockIndex(): Int = activeBlockIndex

    internal fun shouldDrawActiveBlockConnection(): Boolean =
        mouseOverDivider || mouseOverAssemblyGutter || mouseOverSourceConnectionZone || stickyActiveBlockConnection

    private fun blockIndexForSourceLine(line: Int): Int =
        previewFile.presentation.blocks.indexOfFirst { line in it.sourceLines }

    private fun blockIndexForAssemblyLine(line: Int): Int = previewFile.presentation.blocks.indexOfFirst { block ->
        block.assemblyLines.any { line in it }
    }

    private fun sourceConnectionHit(component: Component, point: Point): SourceConnectionHit? {
        val sourceEditor = sourceTextEditor.editor
        val editorPoint = SwingUtilities.convertPoint(component, point, sourceEditor.component)
        if (!isInSourceConnectionHotspot(sourceEditor.component, editorPoint.x)) {
            return null
        }

        val contentPoint = SwingUtilities.convertPoint(component, point, sourceEditor.contentComponent)
        val line = sourceEditor.xyToLogicalPosition(contentPoint).line
        val blockIndex = blockIndexForSourceLine(line)
        if (blockIndex < 0) {
            return null
        }

        return SourceConnectionHit(blockIndex, contentPoint.y)
    }

    private fun isInSourceConnectionHotspot(component: Component, x: Int): Boolean =
        x >= component.width - SOURCE_CONNECTION_HOTSPOT_WIDTH

    private fun updateActiveBlockIndex(blockIndex: Int) {
        if (activeBlockIndex == blockIndex) {
            return
        }

        activeBlockIndex = blockIndex
        if (blockIndex < 0) {
            stickyActiveBlockConnection = false
        }
        repaintConnections()
    }

    private fun repaintConnections() {
        splitter?.repaintDivider()
        (assemblyTextEditor.editor as? EditorEx)?.gutterComponentEx?.repaint()
    }

    private fun scrollAssemblyToSourceBlock(blockIndex: Int, mouseY: Int) {
        val block = previewFile.presentation.blocks.getOrNull(blockIndex) ?: return
        val assemblyRanges = block.assemblyLines
        if (assemblyRanges.isEmpty()) {
            return
        }

        if (blockIndex == previousClickedBlockIndex) {
            lastShownAssemblyRangeIndex += 1
        } else {
            lastShownAssemblyRangeIndex = 0
        }

        val assemblyRange = assemblyRanges[lastShownAssemblyRangeIndex % assemblyRanges.size]
        val assemblyEditor = assemblyTextEditor.editor
        val assemblyDocument = assemblyEditor.document
        val assemblyY = assemblyEditor.offsetToXY(assemblyDocument.getLineStartOffset(assemblyRange.first)).y +
            headerHeight(assemblyEditor)
        assemblyEditor.scrollingModel.scrollVertically(assemblyY - mouseY)
        previousClickedBlockIndex = blockIndex
        stickyActiveBlockConnection = true
        updateActiveBlockIndex(blockIndex)
        repaintConnections()
    }

    private fun scrollAssemblyToSourceBlockFromEditor(blockIndex: Int, mouseY: Int) {
        val block = previewFile.presentation.blocks.getOrNull(blockIndex) ?: return
        val assemblyRanges = block.assemblyLines
        if (assemblyRanges.isEmpty()) {
            return
        }

        if (blockIndex == previousClickedBlockIndex) {
            lastShownAssemblyRangeIndex += 1
        } else {
            lastShownAssemblyRangeIndex = 0
        }

        val assemblyRange = assemblyRanges[lastShownAssemblyRangeIndex % assemblyRanges.size]
        val sourceEditor = sourceTextEditor.editor
        val assemblyEditor = assemblyTextEditor.editor
        val assemblyDocument = assemblyEditor.document
        val assemblyY = assemblyEditor.offsetToXY(assemblyDocument.getLineStartOffset(assemblyRange.first)).y +
            headerHeight(assemblyEditor)
        val relativeY = mouseY - verticalScrollOffsetFromHeader(sourceEditor)
        assemblyEditor.scrollingModel.scrollVertically(assemblyY - relativeY)
        previousClickedBlockIndex = blockIndex
        stickyActiveBlockConnection = true
        updateActiveBlockIndex(blockIndex)
        repaintConnections()
    }

    private fun scrollSourceToAssemblyBlock(blockIndex: Int, mouseY: Int) {
        val block = previewFile.presentation.blocks.getOrNull(blockIndex) ?: return
        val sourceEditor = sourceTextEditor.editor
        val sourceDocument = sourceEditor.document
        val sourceY = sourceEditor.offsetToXY(sourceDocument.getLineStartOffset(block.sourceLines.first)).y +
            headerHeight(sourceEditor)
        val relativeY = mouseY - verticalScrollOffsetFromHeader(assemblyTextEditor.editor)
        sourceEditor.scrollingModel.scrollVertically(sourceY - relativeY)
        stickyActiveBlockConnection = true
        updateActiveBlockIndex(blockIndex)
        repaintConnections()
    }

    private fun lineForDividerY(mouseY: Int): Int {
        val sourceEditor = sourceTextEditor.editor
        val y = verticalScrollOffsetFromHeader(sourceEditor) + mouseY
        return sourceEditor.xyToLogicalPosition(Point(0, y)).line
    }

    private fun verticalScrollOffsetFromHeader(editor: com.intellij.openapi.editor.Editor): Int =
        editor.scrollingModel.verticalScrollOffset - headerHeight(editor)

    private fun headerHeight(editor: com.intellij.openapi.editor.Editor): Int = editor.headerComponent?.height ?: 0

    private fun updateMouseOverSourceConnectionZone(isOver: Boolean) {
        if (mouseOverSourceConnectionZone == isOver) {
            return
        }

        mouseOverSourceConnectionZone = isOver
        repaintConnections()
    }

    private inner class BlockHoverListener(private val isSourceEditor: Boolean) : EditorMouseMotionListener {
        override fun mouseMoved(e: EditorMouseEvent) {
            val editor = e.editor
            val line = editor.xyToLogicalPosition(Point(0, e.mouseEvent.y)).line
            val blockIndex = if (isSourceEditor) {
                blockIndexForSourceLine(line)
            } else {
                blockIndexForAssemblyLine(line)
            }
            updateActiveBlockIndex(blockIndex)
        }
    }

    private inner class SourceConnectionMouseHandler : MouseAdapter() {
        override fun mouseMoved(e: MouseEvent) {
            val hit = sourceConnectionHit(e.component, e.point)
            updateMouseOverSourceConnectionZone(hit != null)
            if (hit != null) {
                updateActiveBlockIndex(hit.blockIndex)
            }
            e.component.cursor = if (hit != null) {
                java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
            } else {
                java.awt.Cursor.getDefaultCursor()
            }
        }

        override fun mouseExited(e: MouseEvent) {
            updateMouseOverSourceConnectionZone(false)
            e.component.cursor = java.awt.Cursor.getDefaultCursor()
        }

        override fun mouseClicked(e: MouseEvent) {
            val hit = sourceConnectionHit(e.component, e.point)
            if (hit != null) {
                scrollAssemblyToSourceBlockFromEditor(hit.blockIndex, hit.sourceMouseY)
            }
        }
    }

    private inner class AssemblyGutterMouseListener : MouseAdapter() {
        override fun mouseEntered(e: MouseEvent) {
            mouseOverAssemblyGutter = true
            repaintConnections()
        }

        override fun mouseExited(e: MouseEvent) {
            mouseOverAssemblyGutter = false
            repaintConnections()
        }
    }

    private inner class DividerMouseHandler : MouseAdapter() {
        override fun mouseEntered(e: MouseEvent) {
            mouseOverDivider = true
            repaintConnections()
        }

        override fun mouseExited(e: MouseEvent) {
            mouseOverDivider = false
            repaintConnections()
        }

        override fun mouseMoved(e: MouseEvent) {
            val blockIndex = blockIndexForSourceLine(lineForDividerY(e.point.y))
            updateActiveBlockIndex(blockIndex)
            e.component.cursor = if (blockIndex >= 0) {
                java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
            } else {
                java.awt.Cursor.getDefaultCursor()
            }
        }

        override fun mouseClicked(e: MouseEvent) {
            val blockIndex = blockIndexForSourceLine(lineForDividerY(e.point.y))
            if (blockIndex >= 0) {
                scrollAssemblyToSourceBlock(blockIndex, e.point.y)
            }
        }
    }

    private inner class AssemblyGutterRenderer(private val blockIndex: Int, private val assemblyRange: IntRange) :
        ActiveGutterRenderer {
        override fun paint(editor: com.intellij.openapi.editor.Editor, g: Graphics, r: Rectangle) {
            val gutter = (editor as? EditorEx)?.gutterComponentEx ?: return
            val colors = TolkAssemblyBlockPalette.colorsFor(blockIndex)
            val expandMarker = blockIndex == activeBlockIndex && shouldDrawActiveBlockConnection()
            if (!expandMarker) {
                return
            }

            // DiffDrawUtil expects an exclusive end line; keep the model inclusive elsewhere.
            val markerRange = DiffDrawUtil.getGutterMarkerPaintRange(
                editor,
                assemblyRange.first,
                assemblyRange.last + 1,
            )

            g.color = colors.dividerFill
            g.fillRect(
                0,
                markerRange.y1,
                gutter.width,
                markerRange.y2 - markerRange.y1,
            )
        }

        override fun canDoAction(editor: com.intellij.openapi.editor.Editor, e: MouseEvent): Boolean = true

        override fun doAction(editor: com.intellij.openapi.editor.Editor, e: MouseEvent) {
            scrollSourceToAssemblyBlock(blockIndex, e.point.y)
        }
    }

    private companion object {
        private const val CLION_DIVIDER_EXTRA_WIDTH = 10
        private const val SOURCE_CONNECTION_HOTSPOT_WIDTH = 96

        private fun createTextEditor(project: Project, file: VirtualFile): TextEditor =
            TextEditorProvider.getInstance().createEditor(project, file) as TextEditor
    }

    private data class SourceConnectionHit(val blockIndex: Int, val sourceMouseY: Int)
}
