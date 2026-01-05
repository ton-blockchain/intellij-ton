package org.ton.intellij.acton.ide

import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.codeInsight.hints.presentation.PresentationRenderer
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.Filter.ResultItem
import com.intellij.execution.impl.InlayProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.wm.ToolWindowManager
import java.awt.Cursor

@Suppress("UnstableApiUsage")
class ActonWalletQuickFixInlay(offset: Int) : ResultItem(offset, offset, null), InlayProvider {
    override fun createInlayRenderer(editor: Editor): EditorCustomElementRenderer {
        val factory = PresentationFactory(editor)
        val items = arrayOf(
            factory.smallScaledIcon(AllIcons.Actions.QuickfixBulb),
            factory.smallText(" Open Wallets")
        )
        val basePresentation = factory.referenceOnHover(factory.roundWithBackground(factory.seq(*items))) { _, _ ->
            val project = editor.project ?: return@referenceOnHover
            ToolWindowManager.getInstance(project).getToolWindow("Acton Wallets")?.show()
        }
        val presentation = factory.withCursorOnHover(basePresentation, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
        return PresentationRenderer(presentation)
    }
}

class ActonWalletQuickFixFilter : Filter, DumbAware {
    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        if (!line.contains("Wallets are not configured yet")) {
            return null
        }

        val item = ActonWalletQuickFixInlay(entireLength - 1)
        return Filter.Result(listOf(item, ResultItem(entireLength - line.length, entireLength - line.length, null)))
    }
}
