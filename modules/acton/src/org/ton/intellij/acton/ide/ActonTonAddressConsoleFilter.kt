package org.ton.intellij.acton.ide

import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class ActonTonAddressConsoleFilter : Filter {
    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        if (line.length > 300) {
            // likely some raw data
            return null
        }
        // Fast check: if the line doesn't contain any of these characters, it can't contain a TON address
        if (!line.contains(':') && !line.contains('Q') && !line.contains('f')) {
            return null
        }

        val results = mutableListOf<Filter.ResultItem>()
        val offset = entireLength - line.length

        // raw addresses
        rawPattern.findAll(line).forEach { match ->
            val address = match.value
            results.add(
                Filter.ResultItem(
                    offset + match.range.first,
                    offset + match.range.last + 1,
                    TonAddressHyperlinkInfo(address, false),
                    getAttrs(), // most lines won't contain addresses, so getting the attributes for each link should be faster.
                )
            )
        }

        // user-friendly addresses
        userFriendlyPattern.findAll(line).forEach { match ->
            val address = match.value
            val isTestnet = address.startsWith("k") || address.startsWith("0")
            results.add(
                Filter.ResultItem(
                    offset + match.range.first,
                    offset + match.range.last + 1,
                    TonAddressHyperlinkInfo(address, isTestnet),
                    getAttrs(),
                )
            )
        }

        return if (results.isEmpty()) null else Filter.Result(results)
    }

    private fun getAttrs(): TextAttributes? {
        val attrs = EditorColorsManager.getInstance().globalScheme.getAttributes(CodeInsightColors.HYPERLINK_ATTRIBUTES)
        return attrs
    }

    companion object {
        private val rawPattern = Regex("""\b-?[0-9]:[0-9a-fA-F]{64}\b""")
        private val userFriendlyPattern = Regex("""\b[EfUk0][Qf][a-zA-Z0-9_-]{46}\b""")
    }
}

class TonAddressHyperlinkInfo(private val address: String, private val isTestnet: Boolean) : HyperlinkInfo {
    override fun navigate(project: Project) {
        val domain = if (isTestnet) "testnet.tonviewer.com" else "tonviewer.com"
        val encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8.toString()).replace("+", "%20")
        BrowserUtil.browse("https://$domain/$encodedAddress")
    }
}
