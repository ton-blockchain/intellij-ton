package org.ton.intellij.acton.ide

import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.codeInsight.hints.presentation.PresentationRenderer
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.Filter.ResultItem
import com.intellij.execution.impl.InlayProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import java.awt.Cursor

private const val WALLET_TOOL_WINDOW_ID = "Acton Wallets"
private const val WALLETS_NOT_CONFIGURED = "Wallets are not configured yet"
private val WALLET_NOT_FOUND_REGEX =
    Regex("""Wallet\s+[`'"]?([A-Za-z0-9_.-]+)[`'"]?\s+not found""", RegexOption.IGNORE_CASE)

@Suppress("UnstableApiUsage")
class ActonWalletQuickFixInlay(offset: Int, private val suggestedWalletName: String?) :
    ResultItem(offset, offset, null),
    InlayProvider {
    override fun createInlayRenderer(editor: Editor): EditorCustomElementRenderer {
        val factory = PresentationFactory(editor)
        val items = arrayOf(
            factory.smallScaledIcon(AllIcons.Actions.QuickfixBulb),
            factory.smallText(" Create wallet"),
        )
        val basePresentation = factory.referenceOnHover(factory.roundWithBackground(factory.seq(*items))) { _, _ ->
            val project = editor.project ?: return@referenceOnHover
            val toolWindow =
                ToolWindowManager.getInstance(project).getToolWindow(WALLET_TOOL_WINDOW_ID) ?: return@referenceOnHover
            toolWindow.show {
                openCreateWalletDialogWhenPanelReady(toolWindow)
            }
        }
        val presentation = factory.withCursorOnHover(basePresentation, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
        return PresentationRenderer(presentation)
    }

    private fun openCreateWalletDialogWhenPanelReady(toolWindow: ToolWindow) {
        val walletPanel = toolWindow.contentManager.selectedContent?.component as? ActonWalletPanel
            ?: toolWindow.contentManager.contents.firstOrNull()?.component as? ActonWalletPanel

        if (walletPanel != null) {
            walletPanel.openCreateWalletDialog(suggestedWalletName, waitForWalletsView = true)
            return
        }

        ApplicationManager.getApplication().invokeLater {
            val delayedWalletPanel = toolWindow.contentManager.selectedContent?.component as? ActonWalletPanel
                ?: toolWindow.contentManager.contents.firstOrNull()?.component as? ActonWalletPanel
            delayedWalletPanel?.openCreateWalletDialog(suggestedWalletName, waitForWalletsView = true)
        }
    }
}

class ActonWalletQuickFixFilter :
    Filter,
    DumbAware {
    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        if (!line.contains(WALLETS_NOT_CONFIGURED)) {
            return null
        }

        val suggestedWalletName = WALLET_NOT_FOUND_REGEX.find(line)?.groupValues?.getOrNull(1)
        val item = ActonWalletQuickFixInlay(entireLength - 1, suggestedWalletName)
        return Filter.Result(listOf(item, ResultItem(entireLength - line.length, entireLength - line.length, null)))
    }
}
