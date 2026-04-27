package org.ton.intellij.tolk.ide.assembly

import com.intellij.diff.tools.util.DiffSplitter
import com.intellij.diff.util.DiffDrawUtil
import com.intellij.ui.JBColor
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.JComponent

class TolkAssemblyDividerPainter(private val previewEditor: TolkAssemblyPreviewEditor) : DiffSplitter.Painter {
    override fun paint(graphics: Graphics, divider: JComponent) {
        val sourceEditor = previewEditor.sourceTextEditor.editor
        val assemblyEditor = previewEditor.assemblyTextEditor.editor
        val blocks = previewEditor.previewFile.presentation.blocks
        val g = graphics.create() as Graphics2D

        try {
            g.color = DiffDrawUtil.getDividerColor(sourceEditor)
            g.fillRect(0, 0, divider.width, divider.height)

            val centerX = divider.width / 2
            g.color = JBColor.border()
            g.drawLine(centerX, 0, centerX, divider.height)

            blocks.forEachIndexed { index, block ->
                val colors = TolkAssemblyBlockPalette.colorsFor(index)
                block.assemblyLines.forEach { assemblyRange ->
                    DiffDrawUtil.drawCurveTrapezium(
                        g,
                        0,
                        divider.width,
                        DiffDrawUtil.lineToY(sourceEditor, block.sourceLines.first),
                        DiffDrawUtil.lineToY(assemblyEditor, assemblyRange.first),
                        DiffDrawUtil.lineToY(sourceEditor, block.sourceLines.last + 1),
                        DiffDrawUtil.lineToY(assemblyEditor, assemblyRange.last + 1),
                        colors.dividerFill,
                        colors.dividerBorder,
                    )
                }
            }
        } finally {
            g.dispose()
        }
    }
}
