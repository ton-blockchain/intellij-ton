package org.ton.intellij.tolk.debug.retrace

import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.codeInsight.hints.presentation.PresentationRenderer
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.Filter.ResultItem
import com.intellij.execution.impl.InlayProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.project.DumbAware
import java.awt.Cursor

@Suppress("UnstableApiUsage")
class TolkRetraceTransactionInlay(
    offset: Int,
    private val transactionHash: String,
    private val network: String
) : ResultItem(offset, offset, null), InlayProvider {
    override fun createInlayRenderer(editor: Editor): EditorCustomElementRenderer {
        val factory = PresentationFactory(editor)
        val items = arrayOf(
            factory.smallScaledIcon(AllIcons.Actions.StartDebugger),
            factory.smallText(" Debug")
        )
        val basePresentation = factory.referenceOnHover(factory.roundWithBackground(factory.seq(*items))) { _, _ ->
            val project = editor.project ?: return@referenceOnHover
            TolkRetraceLauncher.launch(project, transactionHash, network = network)
        }
        val presentation = factory.withCursorOnHover(basePresentation, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
        return PresentationRenderer(presentation)
    }
}

class TolkRetraceTransactionConsoleFilter : Filter, DumbAware {
    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        if (!line.contains("http") || !line.contains("transaction", ignoreCase = true) && !line.contains("/tx/")) {
            return null
        }

        val offset = entireLength - line.length
        val items = TRANSACTION_URL_REGEX.findAll(line)
            .map { match ->
                val host = match.groupValues[1]
                val transactionHash = match.groupValues[2]
                TolkRetraceTransactionInlay(
                    offset + match.range.last + 1,
                    transactionHash,
                    inferNetwork(host)
                )
            }
            .toMutableList<ResultItem>()

        if (items.isEmpty()) {
            return null
        }

        items += ResultItem(offset, offset, null)
        return Filter.Result(items)
    }

    private fun inferNetwork(host: String): String {
        return when {
            host.contains("testnet", ignoreCase = true) -> "testnet"
            host.contains("mainnet", ignoreCase = true) -> "mainnet"
            else -> ""
        }
    }

    companion object {
        private val TRANSACTION_URL_REGEX = Regex(
            """https?://([^/\s]+)/(?:transaction|tx)/([0-9a-fA-F]{64})\b""",
            RegexOption.IGNORE_CASE
        )
    }
}
