package org.ton.intellij.tolk.ide.assembly

import com.intellij.diff.tools.util.DiffSplitter
import com.intellij.diff.util.DiffDividerDrawUtil
import com.intellij.diff.util.DiffDrawUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import java.awt.Graphics
import java.awt.RenderingHints
import javax.swing.JComponent

class TolkAssemblyDividerPainter(private val previewEditor: TolkAssemblyPreviewEditor) : DiffSplitter.Painter {
    override fun paint(graphics: Graphics, divider: JComponent) {
        val sourceEditor = previewEditor.sourceTextEditor.editor
        val assemblyEditor = previewEditor.assemblyTextEditor.editor
        val g = DiffDividerDrawUtil.getDividerGraphics(graphics, divider, sourceEditor.component)

        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.color = DiffDrawUtil.getDividerColor(sourceEditor)
            g.fill(g.clipBounds)

            val blockIndex = previewEditor.activeBlockIndex()
            val block = previewEditor.activeBlock() ?: return
            val colors = TolkAssemblyBlockPalette.colorsFor(blockIndex)
            val (sourceStartY, sourceEndY) = yRange(sourceEditor, block.sourceLines)

            g.color = colors.dividerFill
            g.fillRect(0, sourceStartY, SOURCE_STRIP_WIDTH, sourceEndY - sourceStartY)

            if (!previewEditor.shouldDrawActiveBlockConnection()) {
                return
            }

            block.assemblyLines.forEach { assemblyRange ->
                val (assemblyStartY, assemblyEndY) = yRange(assemblyEditor, assemblyRange)

                DiffDrawUtil.drawCurveTrapezium(
                    g,
                    SOURCE_STRIP_WIDTH,
                    divider.width,
                    sourceStartY,
                    sourceEndY,
                    assemblyStartY,
                    assemblyEndY,
                    colors.dividerFill,
                    null,
                )
            }
        } finally {
            g.dispose()
        }
    }

    private fun yRange(editor: Editor, lines: IntRange): Pair<Int, Int> {
        val lineCount = editor.document.lineCount
        if (lineCount <= 0) {
            return 0 to 0
        }

        val startLine = lines.first.coerceIn(0, lineCount - 1)
        val endLineExclusive = (lines.last + 1).coerceIn(startLine + 1, lineCount)
        val scrollOffset = editor.scrollingModel.verticalScrollOffset - (editor.headerComponent?.height ?: 0)
        val startY = editor.logicalPositionToXY(LogicalPosition(startLine, 0)).y - scrollOffset
        val endY = editor.logicalPositionToXY(LogicalPosition(endLineExclusive, 0)).y - scrollOffset
        return startY to endY
    }

    private companion object {
        private const val SOURCE_STRIP_WIDTH = 10
    }
}
