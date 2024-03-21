package org.ton.intellij.func.doc

import org.intellij.markdown.parser.MarkdownParser
import org.ton.intellij.func.doc.psi.FuncDocComment
import org.ton.intellij.func.ide.quickdoc.MarkdownNode
import org.ton.intellij.util.DocLine
import org.ton.intellij.util.content
import org.ton.intellij.util.removeEolDecoration

/**
 * Render the markdown content of a [FuncDocComment] to HTML.
 * @sample removeEolDecoration
 */
fun FuncDocComment.renderHtml(): String {
    val rawText = DocLine.splitLines(text).removeEolDecoration(";;;").content().joinToString("\n")

    val markdownRoot =
        MarkdownParser(FuncDocMarkdownFlavourDescriptor()).buildMarkdownTreeFromString(
            rawText
        )
    val markdownNode = MarkdownNode(markdownRoot, null, rawText, this)
    return markdownNode.toHtml().also {
        println("rendered = $it")
    }
}
