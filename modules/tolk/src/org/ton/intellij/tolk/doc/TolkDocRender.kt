package org.ton.intellij.tolk.doc

import org.intellij.markdown.parser.MarkdownParser
import org.ton.intellij.tolk.doc.psi.TolkDocComment
import org.ton.intellij.tolk.ide.quickdoc.MarkdownNode
import org.ton.intellij.util.DocLine
import org.ton.intellij.util.content
import org.ton.intellij.util.removeEolDecoration

/**
 * Render the markdown content of a [TolkDocComment] to HTML.
 * @sample removeEolDecoration
 */
fun TolkDocComment.renderHtml(): String {
    val rawText = DocLine.splitLines(text).removeEolDecoration("///").content().joinToString("\n")

    val markdownRoot =
        MarkdownParser(TolkDocMarkdownFlavourDescriptor()).buildMarkdownTreeFromString(
            rawText
        )
    val markdownNode = MarkdownNode(markdownRoot, null, rawText, this)
    return markdownNode.toHtml()
}
